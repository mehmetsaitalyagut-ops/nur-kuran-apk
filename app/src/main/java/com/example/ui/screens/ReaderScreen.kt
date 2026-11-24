package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.FavoriteItemDoc
import com.example.model.Ayah
import com.example.model.SurahInfo
import com.example.player.PlaybackUiState
import com.example.ui.ReaderUiState
import com.example.ui.UiState
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.RubElHizbBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuranArabicTextStyle
import com.example.ui.theme.QuranBismillahTextStyle
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    readerState: ReaderUiState,
    playbackState: PlaybackUiState,
    favoritesState: UiState<List<FavoriteItemDoc>>,
    onLoadSurah: (Int, Int?) -> Unit,
    onConsumeScrollTarget: () -> Unit,
    onPlaySurah: (Int, Int) -> Unit,
    onPlayAyah: (Int, Int) -> Unit,
    onToggleFavoriteSurah: (SurahInfo) -> Unit,
    onToggleFavoriteAyah: (SurahInfo, Ayah) -> Unit,
    onToggleArabic: () -> Unit,
    onToggleTurkish: () -> Unit,
    onAdjustFontScale: (Float) -> Unit,
    onMarkAyahsRead: (Long, SurahInfo, Int) -> Unit,
    bottomContentPadding: Dp
) {
    val surah = readerState.surah
    val ayahs = readerState.content?.ayahs.orEmpty()
    val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()
    val isSurahFav = favorites.any { it.kind == "surah" && it.surahNumber.toInt() == surah.number }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var ayahJumpQuery by remember(surah.number) { mutableStateOf("") }

    val activePlayingAyahNumber = if (
        playbackState.currentSurah?.number == surah.number &&
        (playbackState.isPlaying || playbackState.isBuffering || playbackState.ayahs.isNotEmpty())
    ) {
        playbackState.currentAyahNumber
    } else null

    // Scroll to target ayah when opened from bookmark/resume/search/daily verse
    LaunchedEffect(readerState.scrollToAyahNumber, ayahs.size) {
        val target = readerState.scrollToAyahNumber
        if (target != null && ayahs.isNotEmpty()) {
            val idx = (target - 1).coerceIn(0, ayahs.lastIndex)
            val targetItemIndex = idx + 1 // +1 accounts for the Surah header item at index 0
            if (kotlin.math.abs(listState.firstVisibleItemIndex - targetItemIndex) > 15) {
                listState.scrollToItem(targetItemIndex)
            } else {
                listState.animateScrollToItem(targetItemIndex)
            }
            onConsumeScrollTarget()
        }
    }

    // Gently follow playing ayah while listening in reader
    LaunchedEffect(activePlayingAyahNumber, ayahs.size) {
        if (activePlayingAyahNumber != null && playbackState.isPlaying && ayahs.isNotEmpty()) {
            val idx = (activePlayingAyahNumber - 1).coerceIn(0, ayahs.lastIndex)
            listState.animateScrollToItem(idx + 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Index 0: Surah Header & Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightBorder = true
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val prev = if (surah.number > 1) surah.number - 1 else 114
                                    onLoadSurah(prev, null)
                                },
                                modifier = Modifier.testTag("reader_prev_surah")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Önceki sure")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = surah.nameAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 30.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${surah.number}. ${surah.nameTr} Suresi",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${surah.meaningTr} • ${surah.ayahCount} Ayet • ${surah.revelationTypeTr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val next = if (surah.number < 114) surah.number + 1 else 1
                                    onLoadSurah(next, null)
                                },
                                modifier = Modifier.testTag("reader_next_surah")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Sonraki sure")
                            }
                        }

                        if (surah.number != 1 && surah.number != 9) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = stringResource(R.string.bismillah_ar),
                                    style = QuranBismillahTextStyle.copy(fontSize = 24.sp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPlaySurah(surah.number, 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_play_surah"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sureyi Dinle")
                            }

                            OutlinedButton(
                                onClick = { onToggleFavoriteSurah(surah) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("reader_fav_surah")
                            ) {
                                Icon(
                                    imageVector = if (isSurahFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_favorite),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSurahFav) "Favoride" else "Favorile")
                            }
                        }
                    }
                }

                // Reader Display & Ayah Jump Toolbar
                GlassmorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = readerState.showArabic,
                                    onClick = onToggleArabic,
                                    label = { Text("Arapça") }
                                )
                                FilterChip(
                                    selected = readerState.showTurkish,
                                    onClick = onToggleTurkish,
                                    label = { Text("Türkçe Meal") }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onAdjustFontScale(-0.1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Yazıyı küçült")
                                }
                                Text(
                                    text = "Yazı",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onAdjustFontScale(0.1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Yazıyı büyüt")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ayahJumpQuery,
                                onValueChange = { input ->
                                    ayahJumpQuery = input.filter { it.isDigit() }.take(3)
                                },
                                placeholder = { Text("Ayet no (1-${surah.ayahCount})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val num = ayahJumpQuery.toIntOrNull()
                                        if (num != null && num in 1..ayahs.size) {
                                            scope.launch { listState.animateScrollToItem(num) }
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_ayah_jump_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    val num = ayahJumpQuery.toIntOrNull()
                                    if (num != null && num in 1..ayahs.size) {
                                        scope.launch { listState.animateScrollToItem(num) }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Git")
                            }
                            OutlinedButton(
                                onClick = {
                                    onMarkAyahsRead(surah.ayahCount.toLong(), surah, surah.ayahCount)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("reader_mark_surah_read")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Okudum")
                            }
                        }

                        Text(
                            text = "Kaynak: ${readerState.content?.translationSource ?: "Diyanet İşleri Başkanlığı Meali"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (readerState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "${surah.nameTr} Suresi yükleniyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (readerState.errorMessage != null) {
            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = readerState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { onLoadSurah(surah.number, null) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tekrar Dene")
                        }
                    }
                }
            }
        } else {
            items(ayahs, key = { it.globalNumber }) { ayah ->
                val isPlayingThisAyah = activePlayingAyahNumber == ayah.numberInSurah
                val isAyahFav = favorites.any {
                    it.kind == "ayah" &&
                        it.surahNumber.toInt() == surah.number &&
                        it.ayahNumber.toInt() == ayah.numberInSurah
                }

                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayAyah(surah.number, ayah.numberInSurah) }
                        .testTag("ayah_card_${surah.number}_${ayah.numberInSurah}"),
                    cornerRadius = 18.dp,
                    highlightBorder = isPlayingThisAyah
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isPlayingThisAyah) {
                                    Modifier.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                } else Modifier
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RubElHizbBadge(
                                    numberText = ayah.numberInSurah.toString(),
                                    sizeDp = 36.dp,
                                    isHighlighted = isPlayingThisAyah
                                )
                                if (isPlayingThisAyah) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Dinleniyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onMarkAyahsRead(1L, surah, ayah.numberInSurah) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkAdd,
                                        contentDescription = "Buraya kadar okudum",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleFavoriteAyah(surah, ayah) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("ayah_fav_${surah.number}_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        imageVector = if (isAyahFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = stringResource(R.string.cd_favorite),
                                        tint = if (isAyahFav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onPlayAyah(surah.number, ayah.numberInSurah) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                        .testTag("ayah_play_${surah.number}_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "${ayah.numberInSurah}. ayeti dinle",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (readerState.showArabic) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = ayah.arabicText,
                                    style = QuranArabicTextStyle.copy(
                                        fontSize = (26 * readerState.arabicFontScale).sp,
                                        lineHeight = (46 * readerState.arabicFontScale).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (readerState.showTurkish && ayah.turkishText.isNotBlank()) {
                            Text(
                                text = "${ayah.numberInSurah}. ${ayah.turkishText}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isPlayingThisAyah) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
