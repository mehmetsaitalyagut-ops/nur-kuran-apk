package com.example.player

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.remote.QuranRepository
import com.example.model.Ayah
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.model.SurahInfo
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatSetting(val labelTr: String) {
    NONE("Tekrar Kapalı"),
    REPEAT_SURAH("Sureyi Tekrarla"),
    REPEAT_AYAH("Ayeti Tekrarla")
}

data class PlaybackUiState(
    val currentSurah: SurahInfo? = null,
    val currentReciter: Reciter = QuranCatalog.reciters.first(),
    val ayahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoadingSurahAudio: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val repeatSetting: RepeatSetting = RepeatSetting.NONE,
    val autoNextSurah: Boolean = true,
    val errorMessage: String? = null,
    val audioWarning: String? = null
) {
    val currentAyah: Ayah?
        get() = ayahs.getOrNull(currentAyahIndex)

    val currentAyahNumber: Int
        get() = currentAyah?.numberInSurah ?: (currentAyahIndex + 1)

    val surahProgressFraction: Float
        get() {
            val total = ayahs.size.coerceAtLeast(1)
            val ayahFrac = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            return ((currentAyahIndex + ayahFrac) / total.toFloat()).coerceIn(0f, 1f)
        }
}

class QuranPlayerController(
    context: Context,
    private val quranRepository: QuranRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var fallbackPlayer: ExoPlayer? = null
    private var controllerWatchdogJob: Job? = null
    private var progressJob: Job? = null
    private var loadSurahJob: Job? = null

    var onListeningTick: ((seconds: Long, surah: SurahInfo, reciter: Reciter, ayahNumber: Int, posMs: Long) -> Unit)? = null
    var onSurahStarted: ((surah: SurahInfo, reciter: Reciter, ayahNumber: Int) -> Unit)? = null
    var onAyahChanged: ((surah: SurahInfo, reciter: Reciter, ayahNumber: Int, posMs: Long) -> Unit)? = null
    var onPlaybackSnapshot: ((surah: SurahInfo, reciter: Reciter, ayahNumber: Int, posMs: Long) -> Unit)? = null

    private var accumulatedListenMs = 0L
    private var lastReportedSurahNum = -1
    private var lastReportedAyahNum = -1
    private var usingFallbackCdn = false
    private var lastRequestedAutoPlay = true

    private data class PendingPlayRequest(
        val surahNumber: Int,
        val reciter: Reciter,
        val startAyahNumber: Int,
        val startPositionMs: Long,
        val autoPlay: Boolean
    )

    private var pendingPlayRequest: PendingPlayRequest? = null

    private fun activePlayer(): Player? = mediaController ?: fallbackPlayer

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update {
                it.copy(
                    isPlaying = isPlaying,
                    errorMessage = if (isPlaying) null else it.errorMessage
                )
            }
            if (isPlaying) {
                checkSystemMediaVolume()
            } else {
                emitProgressSnapshot()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_ENDED) {
                handlePlaylistEnded()
            }
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            when (playbackSuppressionReason) {
                Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS -> {
                    _state.update {
                        it.copy(audioWarning = "Ses odağı geçici olarak başka bir uygulama tarafından alındı.")
                    }
                }
                Player.PLAYBACK_SUPPRESSION_REASON_NONE -> {
                    checkSystemMediaVolume()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = activePlayer()
            val idx = player?.currentMediaItemIndex ?: 0
            val pos = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
            val dur = player?.duration?.coerceAtLeast(0L) ?: 0L
            _state.update {
                val safeIdx = idx.coerceIn(0, (it.ayahs.size - 1).coerceAtLeast(0))
                it.copy(
                    currentAyahIndex = safeIdx,
                    positionMs = pos,
                    durationMs = dur
                )
            }
            val currentState = _state.value
            val surah = currentState.currentSurah
            if (surah != null && currentState.ayahs.isNotEmpty()) {
                val ayahNum = currentState.currentAyahNumber
                if (lastReportedSurahNum != surah.number || lastReportedAyahNum != ayahNum) {
                    lastReportedSurahNum = surah.number
                    lastReportedAyahNum = ayahNum
                    onAyahChanged?.invoke(surah, currentState.currentReciter, ayahNum, pos)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error (${error.errorCodeName}) fallbackCdn=$usingFallbackCdn", error)
            val currentState = _state.value
            val surah = currentState.currentSurah
            val ayahs = currentState.ayahs
            val player = activePlayer()
            if (!usingFallbackCdn &&
                surah != null &&
                ayahs.isNotEmpty() &&
                player != null &&
                ayahs.any { it.fallbackAudioUrl.isNotBlank() }
            ) {
                usingFallbackCdn = true
                val targetIndex = player.currentMediaItemIndex
                    .coerceAtLeast(currentState.currentAyahIndex)
                    .coerceIn(0, ayahs.lastIndex)
                Log.w(TAG, "Switching to EveryAyah fallback CDN at ayah index $targetIndex")
                val fallbackItems = buildMediaItems(
                    surahInfo = surah,
                    reciter = currentState.currentReciter,
                    ayahs = ayahs,
                    useFallbackCdn = true
                )
                _state.update {
                    it.copy(
                        isBuffering = true,
                        isLoadingSurahAudio = false,
                        errorMessage = null
                    )
                }
                player.setMediaItems(fallbackItems, targetIndex, 0L)
                applyRepeatModeToPlayer(player, currentState.repeatSetting)
                player.volume = currentState.volume
                player.prepare()
                if (lastRequestedAutoPlay) {
                    player.play()
                }
                return
            }

            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isLoadingSurahAudio = false,
                    errorMessage = "Ses oynatılamadı (${error.errorCodeName}). İnternet bağlantınızı kontrol edip tekrar deneyin."
                )
            }
        }
    }

    init {
        connectController()
        startProgressLoop()
    }

    private fun connectController() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, QuranPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    if (fallbackPlayer != null) {
                        val fallbackActive = (fallbackPlayer?.mediaItemCount ?: 0) > 0
                        if (fallbackActive) {
                            // Direct ExoPlayer fallback is already handling active playback; avoid dual audio.
                            controller.release()
                            return@addListener
                        } else {
                            fallbackPlayer?.removeListener(playerListener)
                            fallbackPlayer?.release()
                            fallbackPlayer = null
                        }
                    }
                    controllerWatchdogJob?.cancel()
                    mediaController = controller
                    controller.addListener(playerListener)
                    controller.volume = _state.value.volume
                    applyRepeatModeToPlayer(controller, _state.value.repeatSetting)
                    val pending = pendingPlayRequest
                    if (pending != null) {
                        pendingPlayRequest = null
                        playSurah(
                            surahNumber = pending.surahNumber,
                            reciter = pending.reciter,
                            startAyahNumber = pending.startAyahNumber,
                            startPositionMs = pending.startPositionMs,
                            autoPlay = pending.autoPlay
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect MediaController, activating direct ExoPlayer fallback", e)
                    activateFallbackPlayerIfNeeded()
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun scheduleControllerWatchdog() {
        if (activePlayer() != null || controllerWatchdogJob?.isActive == true) return
        controllerWatchdogJob = scope.launch {
            delay(CONTROLLER_CONNECT_TIMEOUT_MS)
            if (activePlayer() == null) {
                Log.w(TAG, "MediaController connection timed out; activating direct ExoPlayer fallback")
                activateFallbackPlayerIfNeeded()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun activateFallbackPlayerIfNeeded(): Player {
        activePlayer()?.let { return it }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("NURQuranPlayer/1.0 (Android)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(25_000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(appContext, httpDataSourceFactory)
        )

        val player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.volume = _state.value.volume
        applyRepeatModeToPlayer(player, _state.value.repeatSetting)
        player.addListener(playerListener)
        fallbackPlayer = player

        val pending = pendingPlayRequest
        if (pending != null) {
            pendingPlayRequest = null
            playSurah(
                surahNumber = pending.surahNumber,
                reciter = pending.reciter,
                startAyahNumber = pending.startAyahNumber,
                startPositionMs = pending.startPositionMs,
                autoPlay = pending.autoPlay
            )
        }
        return player
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = activePlayer()
                if (player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val idx = player.currentMediaItemIndex.coerceAtLeast(0)
                    _state.update { current ->
                        current.copy(
                            currentAyahIndex = idx.coerceIn(0, (current.ayahs.size - 1).coerceAtLeast(0)),
                            positionMs = pos,
                            durationMs = dur,
                            isPlaying = player.isPlaying,
                            isBuffering = player.playbackState == Player.STATE_BUFFERING
                        )
                    }
                    val currentState = _state.value
                    if (currentState.audioWarning != null && isSystemMediaAudible() && currentState.volume >= 0.05f) {
                        _state.update { it.copy(audioWarning = null) }
                    }
                    if (player.isPlaying && currentState.currentSurah != null) {
                        accumulatedListenMs += 500L
                        if (accumulatedListenMs >= 5000L) {
                            val sec = accumulatedListenMs / 1000L
                            accumulatedListenMs %= 1000L
                            onListeningTick?.invoke(
                                sec,
                                currentState.currentSurah,
                                currentState.currentReciter,
                                currentState.currentAyahNumber,
                                pos
                            )
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    fun playSurah(
        surahNumber: Int,
        reciter: Reciter = _state.value.currentReciter,
        startAyahNumber: Int = 1,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true
    ) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val surahInfo = QuranCatalog.surahByNumber(safeSurah)
        val clampedStartAyah = startAyahNumber.coerceIn(1, surahInfo.ayahCount)
        val safeStartPos = startPositionMs.coerceAtLeast(0L)
        usingFallbackCdn = false
        lastRequestedAutoPlay = autoPlay

        if (autoPlay) {
            checkSystemMediaVolume()
        }

        if (activePlayer() == null) {
            pendingPlayRequest = PendingPlayRequest(
                surahNumber = safeSurah,
                reciter = reciter,
                startAyahNumber = clampedStartAyah,
                startPositionMs = safeStartPos,
                autoPlay = autoPlay
            )
            scheduleControllerWatchdog()
        }

        loadSurahJob?.cancel()
        loadSurahJob = scope.launch {
            _state.update {
                it.copy(
                    currentSurah = surahInfo,
                    currentReciter = reciter,
                    ayahs = emptyList(),
                    currentAyahIndex = (clampedStartAyah - 1).coerceAtLeast(0),
                    positionMs = safeStartPos,
                    isLoadingSurahAudio = true,
                    errorMessage = null
                )
            }
            val result = quranRepository.getSurahContent(safeSurah, reciter)
            result.onSuccess { content ->
                val ayahs = content.ayahs
                val startIndex = (clampedStartAyah - 1).coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
                val player = activePlayer() ?: if (controllerWatchdogJob?.isActive != true) {
                    activateFallbackPlayerIfNeeded()
                } else {
                    null
                }
                if (player != null && ayahs.isNotEmpty()) {
                    pendingPlayRequest = null
                    val mediaItems = buildMediaItems(
                        surahInfo = surahInfo,
                        reciter = reciter,
                        ayahs = ayahs,
                        useFallbackCdn = usingFallbackCdn
                    )
                    player.setMediaItems(mediaItems, startIndex, safeStartPos)
                    applyRepeatModeToPlayer(player, _state.value.repeatSetting)
                    player.volume = _state.value.volume
                    player.prepare()
                    if (autoPlay) {
                        player.play()
                        checkSystemMediaVolume()
                    }
                    val actualAyahNum = ayahs[startIndex].numberInSurah
                    lastReportedSurahNum = surahInfo.number
                    lastReportedAyahNum = actualAyahNum
                    _state.update {
                        it.copy(
                            currentSurah = surahInfo,
                            currentReciter = reciter,
                            ayahs = ayahs,
                            currentAyahIndex = startIndex,
                            positionMs = safeStartPos,
                            isLoadingSurahAudio = false,
                            errorMessage = null
                        )
                    }
                    if (autoPlay) {
                        onSurahStarted?.invoke(surahInfo, reciter, actualAyahNum)
                        onPlaybackSnapshot?.invoke(surahInfo, reciter, actualAyahNum, safeStartPos)
                    }
                } else if (ayahs.isEmpty()) {
                    _state.update {
                        it.copy(
                            ayahs = ayahs,
                            currentAyahIndex = startIndex,
                            isLoadingSurahAudio = false,
                            errorMessage = "Ayet ses dosyaları bulunamadı."
                        )
                    }
                } else {
                    // Surah text/URLs arrived before MediaController finished connecting; watchdog/onConnect will resume
                    _state.update {
                        it.copy(
                            ayahs = ayahs,
                            currentAyahIndex = startIndex,
                            isLoadingSurahAudio = autoPlay
                        )
                    }
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                pendingPlayRequest = null
                _state.update {
                    it.copy(
                        isLoadingSurahAudio = false,
                        errorMessage = err.message ?: "Sure ses dosyaları yüklenemedi."
                    )
                }
            }
        }
    }

    private fun buildMediaItems(
        surahInfo: SurahInfo,
        reciter: Reciter,
        ayahs: List<Ayah>,
        useFallbackCdn: Boolean
    ): List<MediaItem> = ayahs.map { ayah ->
        val rawUrl = if (useFallbackCdn && ayah.fallbackAudioUrl.isNotBlank()) {
            ayah.fallbackAudioUrl
        } else {
            ayah.audioUrl
        }
        val uri = Uri.parse(rawUrl)
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId("${surahInfo.number}_${ayah.numberInSurah}_${reciter.id}_${if (useFallbackCdn) "ea" else "cn"}")
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("${surahInfo.nameTr} • ${ayah.numberInSurah}. Ayet")
                    .setArtist(reciter.nameTr)
                    .setAlbumTitle("NUR – Kur’an-ı Kerim")
                    .build()
            )
            .build()
    }

    fun selectReciter(reciter: Reciter) {
        val currentSurah = _state.value.currentSurah
        val currentAyah = _state.value.currentAyahNumber
        val wasPlaying = _state.value.isPlaying
        _state.update { it.copy(currentReciter = reciter) }
        if (currentSurah != null) {
            playSurah(
                surahNumber = currentSurah.number,
                reciter = reciter,
                startAyahNumber = currentAyah,
                startPositionMs = 0L,
                autoPlay = wasPlaying
            )
        }
    }

    fun togglePlayPause() {
        val currentState = _state.value
        val currentSurah = currentState.currentSurah
        val player = activePlayer()
        if (player == null) {
            playSurah(
                surahNumber = currentSurah?.number ?: 1,
                reciter = currentState.currentReciter,
                startAyahNumber = currentState.currentAyahNumber.coerceAtLeast(1),
                startPositionMs = currentState.positionMs,
                autoPlay = true
            )
            return
        }
        if (player.mediaItemCount == 0 ||
            currentState.errorMessage != null ||
            player.playbackState == Player.STATE_IDLE
        ) {
            playSurah(
                surahNumber = currentSurah?.number ?: 1,
                reciter = currentState.currentReciter,
                startAyahNumber = currentState.currentAyahNumber.coerceAtLeast(1),
                startPositionMs = 0L,
                autoPlay = true
            )
            return
        }
        if (player.isPlaying) {
            player.pause()
            emitProgressSnapshot()
        } else {
            lastRequestedAutoPlay = true
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0L)
            }
            player.volume = _state.value.volume
            player.play()
            checkSystemMediaVolume()
        }
    }

    fun retryCurrentPlayback() {
        val currentState = _state.value
        val surahNum = currentState.currentSurah?.number ?: 1
        playSurah(
            surahNumber = surahNum,
            reciter = currentState.currentReciter,
            startAyahNumber = currentState.currentAyahNumber.coerceAtLeast(1),
            startPositionMs = 0L,
            autoPlay = true
        )
    }

    fun stopPlayback() {
        val player = activePlayer() ?: return
        val current = _state.value
        val surah = current.currentSurah
        val ayahNum = current.currentAyahNumber
        if (surah != null) {
            onPlaybackSnapshot?.invoke(surah, current.currentReciter, ayahNum, 0L)
        }
        player.pause()
        player.seekTo(0, 0L)
        _state.update { it.copy(isPlaying = false, positionMs = 0L, currentAyahIndex = 0) }
    }

    fun seekToAyah(ayahNumber: Int) {
        val current = _state.value
        val ayahs = current.ayahs
        val player = activePlayer()
        if (player == null || ayahs.isEmpty()) {
            current.currentSurah?.let { surah ->
                playSurah(surah.number, current.currentReciter, ayahNumber, 0L, autoPlay = true)
            }
            return
        }
        val idx = (ayahNumber - 1).coerceIn(0, ayahs.lastIndex)
        lastRequestedAutoPlay = true
        player.seekTo(idx, 0L)
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        if (!player.isPlaying) {
            player.play()
        }
        checkSystemMediaVolume()
        _state.update { it.copy(currentAyahIndex = idx, positionMs = 0L, errorMessage = null) }
        val surah = current.currentSurah
        val resolvedAyah = ayahs[idx].numberInSurah
        if (surah != null) {
            lastReportedSurahNum = surah.number
            lastReportedAyahNum = resolvedAyah
            onAyahChanged?.invoke(surah, current.currentReciter, resolvedAyah, 0L)
        }
    }

    fun seekBack10s() {
        val player = activePlayer() ?: return
        val pos = player.currentPosition
        if (pos >= 10_000L) {
            player.seekTo(pos - 10_000L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        emitProgressSnapshot()
    }

    fun seekForward10s() {
        val player = activePlayer() ?: return
        val pos = player.currentPosition
        val dur = player.duration.coerceAtLeast(0L)
        if (dur > 0L && pos + 10_000L < dur) {
            player.seekTo(pos + 10_000L)
        } else if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (dur > 0L) {
            player.seekTo(dur)
        }
        emitProgressSnapshot()
    }

    fun seekToSurahFraction(fraction: Float) {
        val player = activePlayer() ?: return
        val total = _state.value.ayahs.size
        if (total == 0) return
        val clamped = fraction.coerceIn(0f, 0.999f)
        val targetIndex = (clamped * total).toInt().coerceIn(0, total - 1)
        val subFraction = (clamped * total) - targetIndex
        if (targetIndex != player.currentMediaItemIndex) {
            player.seekTo(targetIndex, 0L)
        } else {
            val dur = player.duration.coerceAtLeast(0L)
            if (dur > 0L) {
                player.seekTo((dur * subFraction).toLong())
            }
        }
        emitProgressSnapshot()
    }

    fun previousSurah() {
        val current = _state.value.currentSurah?.number ?: 1
        val prev = if (current > 1) current - 1 else 114
        playSurah(prev, _state.value.currentReciter, 1, 0L, autoPlay = true)
    }

    fun nextSurah() {
        val current = _state.value.currentSurah?.number ?: 1
        val next = if (current < 114) current + 1 else 1
        playSurah(next, _state.value.currentReciter, 1, 0L, autoPlay = true)
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        activePlayer()?.volume = v
        _state.update { current ->
            val clearedWarning = if (v >= 0.05f && current.audioWarning?.contains("Oynatıcı ses seviyesi") == true) {
                null
            } else {
                current.audioWarning
            }
            current.copy(volume = v, audioWarning = clearedWarning)
        }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatSetting) {
            RepeatSetting.NONE -> RepeatSetting.REPEAT_SURAH
            RepeatSetting.REPEAT_SURAH -> RepeatSetting.REPEAT_AYAH
            RepeatSetting.REPEAT_AYAH -> RepeatSetting.NONE
        }
        activePlayer()?.let { applyRepeatModeToPlayer(it, next) }
        _state.update { it.copy(repeatSetting = next) }
    }

    fun toggleAutoNextSurah() {
        _state.update { it.copy(autoNextSurah = !it.autoNextSurah) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearAudioWarning() {
        _state.update { it.copy(audioWarning = null) }
    }

    fun emitProgressSnapshot() {
        val current = _state.value
        val surah = current.currentSurah ?: return
        val player = activePlayer()
        val pos = player?.currentPosition?.coerceAtLeast(0L) ?: current.positionMs
        val idx = player?.currentMediaItemIndex?.coerceIn(0, (current.ayahs.size - 1).coerceAtLeast(0))
            ?: current.currentAyahIndex
        val ayahNum = current.ayahs.getOrNull(idx)?.numberInSurah ?: (idx + 1)
        onPlaybackSnapshot?.invoke(surah, current.currentReciter, ayahNum, pos)
    }

    private fun isSystemMediaAudible(): Boolean {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        val streamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        return streamVol > 0 && !isMuted
    }

    private fun checkSystemMediaVolume() {
        val warning = when {
            !isSystemMediaAudible() ->
                "Cihazın medya sesi kapalı veya sıfırda görünüyor. Lütfen ses açma tuşuna basın."
            _state.value.volume < 0.05f ->
                "Oynatıcı ses seviyesi sıfırda. Oynatıcıdaki ses çubuğunu yükseltin."
            else -> null
        }
        _state.update { it.copy(audioWarning = warning) }
    }

    private fun applyRepeatModeToPlayer(player: Player, setting: RepeatSetting) {
        player.repeatMode = when (setting) {
            RepeatSetting.NONE -> Player.REPEAT_MODE_OFF
            RepeatSetting.REPEAT_SURAH -> Player.REPEAT_MODE_ALL
            RepeatSetting.REPEAT_AYAH -> Player.REPEAT_MODE_ONE
        }
    }

    private fun handlePlaylistEnded() {
        val currentState = _state.value
        if (currentState.repeatSetting == RepeatSetting.NONE && currentState.autoNextSurah) {
            val currentSurah = currentState.currentSurah?.number ?: return
            if (currentSurah < 114) {
                playSurah(currentSurah + 1, currentState.currentReciter, 1, 0L, autoPlay = true)
            }
        }
    }

    fun release() {
        emitProgressSnapshot()
        controllerWatchdogJob?.cancel()
        progressJob?.cancel()
        loadSurahJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        fallbackPlayer?.removeListener(playerListener)
        fallbackPlayer?.release()
        fallbackPlayer = null
    }

    private companion object {
        const val TAG = "QuranPlayerController"
        const val CONTROLLER_CONNECT_TIMEOUT_MS = 2200L
    }
}
