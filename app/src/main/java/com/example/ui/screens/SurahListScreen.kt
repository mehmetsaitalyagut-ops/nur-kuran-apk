package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FavoriteItemDoc
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.model.SearchResultItem
import com.example.model.SurahInfo
import com.example.ui.SearchUiState
import com.example.ui.UiState
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.AmiriFontFamily

private enum class SurahFilter(val labelTr: String) {
    ALL("Tümü (114)"),
    POPULAR("Popüler"),
    MECCAN("Mekki"),
    MEDINAN("Medeni")
}

@Composable
fun SurahListScreen(
    searchState: SearchUiState,
    favoritesState: UiState<List<FavoriteItemDoc>>,
    activeSurahNumber: Int?,
    activeReciter: Reciter,
    onSearchQueryChange: (String) -> Unit,
    onSelectReciter: (Reciter) -> Unit,
    onOpenSurahReader: (Int, Int?) -> Unit,
    onPlaySurah: (Int, Int) -> Unit,
    onToggleFavoriteSurah: (SurahInfo) -> Unit,
    bottomContentPadding: Dp
) {
    var selectedFilter by remember { mutableStateOf(SurahFilter.ALL) }
    val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()

    val filteredSurahs = remember(selectedFilter) {
        when (selectedFilter) {
            SurahFilter.ALL -> QuranCatalog.surahs
            SurahFilter.POPULAR -> QuranCatalog.surahs.filter { it.isPopular }
            SurahFilter.MECCAN -> QuranCatalog.surahs.filter { it.revelationTypeTr == "Mekki" }
            SurahFilter.MEDINAN -> QuranCatalog.surahs.filter { it.revelationTypeTr == "Medeni" }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Sureler & Akıllı Arama",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sure adı, numara, 2:255 ayet kodu, meal, Arapça veya hafız arayın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Smart Search Field
        item {
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Örn: Yasin, 36, 2:255, rahmet, Sudeys...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Aramayı temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_search_input")
            )
        }

        // Active Reciter Selector Strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuranCatalog.reciters, key = { it.id }) { reciter ->
                    val selected = reciter.id == activeReciter.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectReciter(reciter) },
                        label = { Text(reciter.nameTr) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("surah_list_reciter_${reciter.id}")
                    )
                }
            }
        }

        if (searchState.query.isNotBlank()) {
            // Search Results Mode
            if (searchState.isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Kur’an-ı Kerim’de aranıyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (searchState.errorMessage != null) {
                item {
                    Text(
                        text = searchState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!searchState.isSearching && searchState.results.isEmpty() && searchState.errorMessage == null) {
                item {
                    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "“${searchState.query}” için sonuç bulunamadı. Sure adı, sure no, 2:255 formatı veya meal kelimesi deneyin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(searchState.results, key = { item ->
                when (item) {
                    is SearchResultItem.SurahMatch -> "s_${item.surah.number}"
                    is SearchResultItem.ReciterMatch -> "r_${item.reciter.id}"
                    is SearchResultItem.AyahMatch -> "a_${item.surahNumber}_${item.ayahNumber}_${item.isArabic}"
                }
            }) { result ->
                when (result) {
                    is SearchResultItem.ReciterMatch -> {
                        val isSelected = result.reciter.id == activeReciter.id
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectReciter(result.reciter) },
                            highlightBorder = isSelected
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hafız: ${result.reciter.nameTr}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${result.reciter.titleTr} • ${result.reciter.styleTr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isSelected) "Seçili" else "Seç",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    is SearchResultItem.SurahMatch -> {
                        val isFav = favorites.any { it.kind == "surah" && it.surahNumber.toInt() == result.surah.number }
                        SurahRowCard(
                            surah = result.surah,
                            isFavorite = isFav,
                            isPlayingThis = activeSurahNumber == result.surah.number,
                            onOpenReader = { onOpenSurahReader(result.surah.number, null) },
                            onPlay = { onPlaySurah(result.surah.number, 1) },
                            onToggleFavorite = { onToggleFavoriteSurah(result.surah) }
                        )
                    }

                    is SearchResultItem.AyahMatch -> {
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSurahReader(result.surahNumber, result.ayahNumber) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${result.surahNameTr} (${result.surahNumber}:${result.ayahNumber})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPlaySurah(result.surahNumber, result.ayahNumber) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Ayeti dinle",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (result.isArabic) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = result.matchedText,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 20.sp,
                                            lineHeight = 34.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Text(
                                        text = result.matchedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SurahFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.labelTr) }
                        )
                    }
                }
            }

            items(filteredSurahs, key = { it.number }) { surah ->
                val isFav = favorites.any { it.kind == "surah" && it.surahNumber.toInt() == surah.number }
                SurahRowCard(
                    surah = surah,
                    isFavorite = isFav,
                    isPlayingThis = activeSurahNumber == surah.number,
                    onOpenReader = { onOpenSurahReader(surah.number, null) },
                    onPlay = { onPlaySurah(surah.number, 1) },
                    onToggleFavorite = { onToggleFavoriteSurah(surah) }
                )
            }
        }
    }
}
