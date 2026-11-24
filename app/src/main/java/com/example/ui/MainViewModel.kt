package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DailyIbadatDoc
import com.example.data.firebase.FavoriteItemDoc
import com.example.data.firebase.ListeningHistoryDoc
import com.example.data.firebase.UserDataRepository
import com.example.data.firebase.UserPreferencesDoc
import com.example.data.local.LastListenedProgress
import com.example.data.local.LastListenedStore
import com.example.data.remote.QuranRepository
import com.example.model.Ayah
import com.example.model.DailyVerse
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.model.SearchResultItem
import com.example.model.SurahContent
import com.example.model.SurahInfo
import com.example.player.PlaybackUiState
import com.example.player.QuranPlayerController
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ReaderUiState(
    val surah: SurahInfo = QuranCatalog.surahs.first(),
    val content: SurahContent? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scrollToAyahNumber: Int? = null,
    val showArabic: Boolean = true,
    val showTurkish: Boolean = true,
    val arabicFontScale: Float = 1.0f
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResultItem> = emptyList(),
    val errorMessage: String? = null
)

class MainViewModel(
    private val userId: String,
    private val userDataRepository: UserDataRepository,
    private val quranRepository: QuranRepository,
    private val lastListenedStore: LastListenedStore,
    val playerController: QuranPlayerController
) : ViewModel() {

    private val ioPersistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferencesState: StateFlow<UiState<UserPreferencesDoc>> =
        userDataRepository.observePreferences(userId)
            .map<UserPreferencesDoc, UiState<UserPreferencesDoc>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing preferences", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Tercihler yüklenemedi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val favoritesState: StateFlow<UiState<List<FavoriteItemDoc>>> =
        userDataRepository.observeFavorites(userId)
            .map<List<FavoriteItemDoc>, UiState<List<FavoriteItemDoc>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing favorites", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Favoriler yüklenemedi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val historyState: StateFlow<UiState<List<ListeningHistoryDoc>>> =
        userDataRepository.observeHistory(userId)
            .map<List<ListeningHistoryDoc>, UiState<List<ListeningHistoryDoc>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing history", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Dinleme geçmişi yüklenemedi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val dailyIbadatState: StateFlow<UiState<List<DailyIbadatDoc>>> =
        userDataRepository.observeDailyIbadat(userId)
            .map<List<DailyIbadatDoc>, UiState<List<DailyIbadatDoc>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing daily ibadat", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "İbadet takibi yüklenemedi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val playbackState: StateFlow<PlaybackUiState> = playerController.state

    private val _lastListenedState = MutableStateFlow(
        lastListenedStore.load(userId) ?: LastListenedProgress()
    )
    val lastListenedState: StateFlow<LastListenedProgress> = _lastListenedState.asStateFlow()

    private val _readerState = MutableStateFlow(ReaderUiState())
    val readerState: StateFlow<ReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _dailyVerse = MutableStateFlow(
        QuranCatalog.verseOfToday(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
    )
    val dailyVerse: StateFlow<DailyVerse> = _dailyVerse.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var searchJob: Job? = null
    private var readerLoadJob: Job? = null
    private var syncedInitialPref = false
    private var userChangedReciterThisSession = false

    init {
        viewModelScope.launch {
            userDataRepository.ensureUserPreferences(userId)
        }
        viewModelScope.launch {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            _dailyVerse.value = quranRepository.getDailyVerse(dayOfYear)
        }
        viewModelScope.launch {
            preferencesState.collect { prefUi ->
                if (prefUi is UiState.Success) {
                    val pref = prefUi.data
                    val local = lastListenedStore.load(userId)
                    if (local != null && !local.syncedToCloud) {
                        // Local has newer progress captured right before app close/kill; push to cloud
                        val localSurah = QuranCatalog.surahByNumber(local.surahNumber)
                        val localReciter = QuranCatalog.reciterById(local.reciterId)
                        ioPersistScope.launch {
                            userDataRepository.updateLastProgress(
                                userId = userId,
                                surahNumber = localSurah.number,
                                surahNameTr = localSurah.nameTr,
                                ayahNumber = local.ayahNumber,
                                reciterId = localReciter.id,
                                positionMs = local.positionMs
                            ).onSuccess {
                                lastListenedStore.markSynced(userId, local.updatedAtMs)
                                _lastListenedState.update { current ->
                                    if (current.updatedAtMs <= local.updatedAtMs) current.copy(syncedToCloud = true) else current
                                }
                            }
                        }
                    } else {
                        val cloudSurahNum = pref.lastSurahNumber.toInt().coerceIn(1, 114)
                        val cloudSurah = QuranCatalog.surahByNumber(cloudSurahNum)
                        val cloudAyah = pref.lastAyahNumber.toInt().coerceIn(1, cloudSurah.ayahCount)
                        val cloudHasProgress = local?.hasSavedRecord == true ||
                            cloudSurahNum > 1 ||
                            cloudAyah > 1 ||
                            pref.lastPlaybackPositionMs > 0L
                        val merged = LastListenedProgress(
                            surahNumber = cloudSurah.number,
                            surahNameTr = pref.lastSurahNameTr.ifBlank { cloudSurah.nameTr },
                            ayahNumber = cloudAyah,
                            reciterId = pref.lastReciterId.ifBlank { pref.selectedReciterId },
                            positionMs = pref.lastPlaybackPositionMs.coerceAtLeast(0L),
                            updatedAtMs = pref.updatedAt?.toDate()?.time ?: (local?.updatedAtMs ?: 0L),
                            hasSavedRecord = cloudHasProgress,
                            syncedToCloud = true
                        )
                        _lastListenedState.value = merged
                        if (cloudHasProgress) {
                            lastListenedStore.save(userId, merged, synchronous = false)
                        }
                    }

                    if (!syncedInitialPref) {
                        syncedInitialPref = true
                        val reciter = QuranCatalog.reciterById(pref.selectedReciterId)
                        if (!userChangedReciterThisSession) {
                            playerController.selectReciter(reciter)
                        }
                        val target = _lastListenedState.value
                        loadSurahForReading(target.surahNumber, target.ayahNumber)
                    }
                }
            }
        }
        playerController.onSurahStarted = { surah, reciter, ayahNumber ->
            persistLastListened(
                surah = surah,
                ayahNumber = ayahNumber,
                reciter = reciter,
                positionMs = playerController.state.value.positionMs,
                synchronousLocal = false
            )
            ioPersistScope.launch {
                userDataRepository.recordListeningHistory(userId, surah, reciter, ayahNumber)
            }
        }
        playerController.onAyahChanged = { surah, reciter, ayahNumber, posMs ->
            persistLastListened(
                surah = surah,
                ayahNumber = ayahNumber,
                reciter = reciter,
                positionMs = posMs,
                synchronousLocal = false
            )
            ioPersistScope.launch {
                userDataRepository.updateListeningHistoryAyah(userId, surah, reciter, ayahNumber)
            }
        }
        playerController.onPlaybackSnapshot = { surah, reciter, ayahNumber, posMs ->
            persistLastListened(
                surah = surah,
                ayahNumber = ayahNumber,
                reciter = reciter,
                positionMs = posMs,
                synchronousLocal = true
            )
            ioPersistScope.launch {
                userDataRepository.updateListeningHistoryAyah(userId, surah, reciter, ayahNumber)
            }
        }
        playerController.onListeningTick = { seconds, surah, reciter, ayahNumber, posMs ->
            persistLastListened(
                surah = surah,
                ayahNumber = ayahNumber,
                reciter = reciter,
                positionMs = posMs,
                synchronousLocal = false
            )
            ioPersistScope.launch {
                val pref = (preferencesState.value as? UiState.Success)?.data
                val readGoal = pref?.dailyReadGoalAyahs ?: 20L
                val listenGoal = pref?.dailyListenGoalMinutes ?: 15L
                userDataRepository.recordDailyProgress(
                    userId = userId,
                    ayahsDelta = 0L,
                    secondsDelta = seconds,
                    readGoalAyahs = readGoal,
                    listenGoalMinutes = listenGoal
                )
            }
        }
    }

    private fun persistLastListened(
        surah: SurahInfo,
        ayahNumber: Int,
        reciter: Reciter,
        positionMs: Long,
        synchronousLocal: Boolean
    ) {
        val clampedAyah = ayahNumber.coerceIn(1, surah.ayahCount)
        val safePos = positionMs.coerceAtLeast(0L)
        val now = System.currentTimeMillis()
        val progress = LastListenedProgress(
            surahNumber = surah.number,
            surahNameTr = surah.nameTr,
            ayahNumber = clampedAyah,
            reciterId = reciter.id,
            positionMs = safePos,
            updatedAtMs = now,
            hasSavedRecord = true,
            syncedToCloud = false
        )
        lastListenedStore.save(userId, progress, synchronous = synchronousLocal)
        _lastListenedState.value = progress

        ioPersistScope.launch {
            userDataRepository.updateLastProgress(
                userId = userId,
                surahNumber = surah.number,
                surahNameTr = surah.nameTr,
                ayahNumber = clampedAyah,
                reciterId = reciter.id,
                positionMs = safePos
            ).onSuccess {
                lastListenedStore.markSynced(userId, now)
                _lastListenedState.update { current ->
                    if (current.updatedAtMs <= now) current.copy(syncedToCloud = true) else current
                }
            }
        }
    }

    fun flushListeningProgress() {
        playerController.emitProgressSnapshot()
    }

    fun resumeLastListened() {
        val progress = lastListenedState.value
        val surahNum = progress.surahNumber.coerceIn(1, 114)
        val surahInfo = QuranCatalog.surahByNumber(surahNum)
        val ayahNum = progress.ayahNumber.coerceIn(1, surahInfo.ayahCount)
        val reciter = if (userChangedReciterThisSession) {
            currentSelectedReciter()
        } else {
            QuranCatalog.reciterById(progress.reciterId)
        }

        loadSurahForReading(surahNum, ayahNum)

        val currentPlayback = playerController.state.value
        val sameLoaded = currentPlayback.currentSurah?.number == surahNum &&
            currentPlayback.currentReciter.id == reciter.id &&
            currentPlayback.ayahs.isNotEmpty()
        if (sameLoaded && currentPlayback.currentAyahNumber == ayahNum) {
            if (!currentPlayback.isPlaying) {
                playerController.togglePlayPause()
            }
        } else {
            playerController.playSurah(
                surahNumber = surahNum,
                reciter = reciter,
                startAyahNumber = ayahNum,
                startPositionMs = progress.positionMs,
                autoPlay = true
            )
        }
    }

    fun loadSurahForReading(surahNumber: Int, scrollToAyah: Int? = null) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val surahInfo = QuranCatalog.surahByNumber(safeSurah)
        val reciter = playerController.state.value.currentReciter
        readerLoadJob?.cancel()
        readerLoadJob = viewModelScope.launch {
            _readerState.update {
                it.copy(
                    surah = surahInfo,
                    isLoading = true,
                    errorMessage = null,
                    scrollToAyahNumber = scrollToAyah
                )
            }
            val result = quranRepository.getSurahContent(safeSurah, reciter)
            result.onSuccess { content ->
                _readerState.update {
                    it.copy(
                        surah = surahInfo,
                        content = content,
                        isLoading = false,
                        errorMessage = null,
                        scrollToAyahNumber = scrollToAyah
                    )
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _readerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Sure metni yüklenemedi."
                    )
                }
            }
        }
    }

    fun consumeReaderScrollTarget() {
        _readerState.update { it.copy(scrollToAyahNumber = null) }
    }

    fun playSurah(surahNumber: Int, startAyahNumber: Int = 1) {
        val reciter = currentSelectedReciter()
        playerController.playSurah(surahNumber, reciter, startAyahNumber, 0L, autoPlay = true)
    }

    fun playAyahInSurah(surahNumber: Int, ayahNumber: Int) {
        val reciter = currentSelectedReciter()
        val pState = playerController.state.value
        if (pState.currentSurah?.number == surahNumber && pState.ayahs.isNotEmpty()) {
            playerController.seekToAyah(ayahNumber)
        } else {
            playerController.playSurah(surahNumber, reciter, ayahNumber, 0L, autoPlay = true)
        }
        val surah = QuranCatalog.surahByNumber(surahNumber)
        persistLastListened(surah, ayahNumber, reciter, 0L, synchronousLocal = false)
    }

    fun selectReciter(reciter: Reciter) {
        userChangedReciterThisSession = true
        playerController.selectReciter(reciter)
        viewModelScope.launch {
            userDataRepository.updateSelectedReciter(userId, reciter.id)
                .onFailure { _statusBanner.value = "Hafız tercihi kaydedilemedi." }
        }
    }

    fun toggleTheme() {
        val currentDark = (preferencesState.value as? UiState.Success)?.data?.darkTheme ?: true
        viewModelScope.launch {
            userDataRepository.updateTheme(userId, !currentDark)
                .onFailure { _statusBanner.value = "Tema ayarı kaydedilemedi." }
        }
    }

    fun toggleReaderArabic() {
        _readerState.update {
            val next = !it.showArabic
            if (!next && !it.showTurkish) it.copy(showArabic = true) else it.copy(showArabic = next)
        }
    }

    fun toggleReaderTurkish() {
        _readerState.update {
            val next = !it.showTurkish
            if (!next && !it.showArabic) it.copy(showTurkish = true) else it.copy(showTurkish = next)
        }
    }

    fun adjustArabicFontScale(delta: Float) {
        _readerState.update {
            it.copy(arabicFontScale = (it.arabicFontScale + delta).coerceIn(0.8f, 1.5f))
        }
    }

    fun toggleFavoriteSurah(surah: SurahInfo) {
        val favs = (favoritesState.value as? UiState.Success)?.data.orEmpty()
        val isFav = favs.any { it.kind == "surah" && it.surahNumber.toInt() == surah.number }
        viewModelScope.launch {
            userDataRepository.toggleFavoriteSurah(userId, surah, isFav)
                .onFailure { _statusBanner.value = "Favori güncellenemedi." }
        }
    }

    fun toggleFavoriteAyah(surah: SurahInfo, ayah: Ayah) {
        val favs = (favoritesState.value as? UiState.Success)?.data.orEmpty()
        val isFav = favs.any {
            it.kind == "ayah" && it.surahNumber.toInt() == surah.number && it.ayahNumber.toInt() == ayah.numberInSurah
        }
        viewModelScope.launch {
            userDataRepository.toggleFavoriteAyah(userId, surah, ayah, isFav)
                .onFailure { _statusBanner.value = "Ayet favorisi güncellenemedi." }
        }
    }

    fun removeFavorite(favId: String) {
        viewModelScope.launch {
            userDataRepository.removeFavoriteById(userId, favId)
                .onFailure { _statusBanner.value = "Favori kaldırılamadı." }
        }
    }

    fun markAyahsRead(ayahCount: Long, surah: SurahInfo, lastAyahNumber: Int) {
        viewModelScope.launch {
            val pref = (preferencesState.value as? UiState.Success)?.data
            val readGoal = pref?.dailyReadGoalAyahs ?: 20L
            val listenGoal = pref?.dailyListenGoalMinutes ?: 15L
            val reciter = currentSelectedReciter()
            userDataRepository.recordDailyProgress(
                userId = userId,
                ayahsDelta = ayahCount.coerceAtLeast(1L),
                secondsDelta = 0L,
                readGoalAyahs = readGoal,
                listenGoalMinutes = listenGoal
            ).onSuccess {
                _statusBanner.value = "$ayahCount ayet okuma hedefinize eklendi."
            }.onFailure {
                _statusBanner.value = "Okuma ilerlemesi kaydedilemedi."
            }
            persistLastListened(surah, lastAyahNumber, reciter, 0L, synchronousLocal = false)
        }
    }

    fun toggleJuzCompleted(juzNumber: Int) {
        val currentList = (preferencesState.value as? UiState.Success)?.data?.completedJuzs.orEmpty()
        viewModelScope.launch {
            userDataRepository.toggleJuzCompleted(userId, juzNumber, currentList)
                .onFailure { _statusBanner.value = "Cüz durumu güncellenemedi." }
        }
    }

    fun updateDailyGoals(readGoalAyahs: Long, listenGoalMinutes: Long) {
        viewModelScope.launch {
            userDataRepository.updateGoals(userId, readGoalAyahs, listenGoalMinutes)
                .onSuccess { _statusBanner.value = "Günlük hedefleriniz güncellendi." }
                .onFailure { _statusBanner.value = "Hedefler kaydedilemedi." }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchState.update { it.copy(query = newQuery, errorMessage = null) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _searchState.update { it.copy(isSearching = false, results = emptyList(), errorMessage = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true) }
            delay(320L)
            quranRepository.smartSearch(newQuery)
                .onSuccess { items ->
                    _searchState.update { it.copy(isSearching = false, results = items, errorMessage = null) }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    _searchState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = err.message ?: "Arama yapılamadı."
                        )
                    }
                }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    private fun currentSelectedReciter(): Reciter {
        val prefId = (preferencesState.value as? UiState.Success)?.data?.selectedReciterId
        return if (userChangedReciterThisSession || prefId == null) {
            playerController.state.value.currentReciter
        } else {
            QuranCatalog.reciterById(prefId)
        }
    }

    override fun onCleared() {
        playerController.emitProgressSnapshot()
        playerController.onListeningTick = null
        playerController.onSurahStarted = null
        playerController.onAyahChanged = null
        playerController.onPlaybackSnapshot = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
