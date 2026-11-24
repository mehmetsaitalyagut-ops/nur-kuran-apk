package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.firebase.DailyIbadatDoc
import com.example.data.firebase.FavoriteItemDoc
import com.example.data.firebase.ListeningHistoryDoc
import com.example.data.firebase.UserDataRepository
import com.example.data.local.LastListenedProgress
import com.example.model.QuranCatalog
import com.example.ui.UiState
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.RubElHizbBadge
import com.example.ui.theme.AmiriFontFamily

private enum class PersonalTab(val labelTr: String) {
    FAV_SURAHS("Favori Sureler"),
    FAV_AYAHS("Favori Ayetler"),
    HISTORY("Dinleme Geçmişi")
}

@Composable
fun FavoritesScreen(
    lastProgress: LastListenedProgress,
    favoritesState: UiState<List<FavoriteItemDoc>>,
    historyState: UiState<List<ListeningHistoryDoc>>,
    dailyIbadatState: UiState<List<DailyIbadatDoc>>,
    onResumeLastListened: () -> Unit,
    onOpenSurahReader: (Int, Int?) -> Unit,
    onPlaySurah: (Int, Int) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    bottomContentPadding: Dp
) {
    var selectedTab by remember { mutableStateOf(PersonalTab.FAV_SURAHS) }
    val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()
    val history = (historyState as? UiState.Success)?.data.orEmpty()
    val ibadatList = (dailyIbadatState as? UiState.Success)?.data.orEmpty()

    val todayRecord = ibadatList.firstOrNull { it.dateKey == UserDataRepository.todayKey() }
    val todaySeconds = todayRecord?.listenedSeconds ?: 0L
    val todayMin = todaySeconds / 60L
    val todayRemSec = todaySeconds % 60L

    val favSurahs = favorites.filter { it.kind == "surah" }
    val favAyahs = favorites.filter { it.kind == "ayah" }

    val lastSurahNum = lastProgress.surahNumber.coerceIn(1, 114)
    val lastSurah = QuranCatalog.surahByNumber(lastSurahNum)
    val lastAyahNum = lastProgress.ayahNumber.coerceIn(1, lastSurah.ayahCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Kişisel Alan & Favoriler",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Resume & Daily Listening Duration Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable { onResumeLastListened() },
                    highlightBorder = true
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kaldığın Yer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "${lastSurah.nameTr} • $lastAyahNum. Ayet",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenSurahReader(lastSurah.number, lastAyahNum) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Oku", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onResumeLastListened,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dinle", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                GlassmorphicCard(modifier = Modifier.weight(0.9f)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bugün",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "${todayMin}dk ${todayRemSec}sn",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dinleme süresi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonalTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.labelTr, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("personal_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedTab) {
            PersonalTab.FAV_SURAHS -> {
                if (favSurahs.isEmpty()) {
                    item {
                        EmptyPersonalCard(
                            icon = Icons.Rounded.Favorite,
                            title = "Henüz favori suren yok",
                            subtitle = "Sure listesinde veya okuyucuda kalp simgesine dokunarak favori surelerini buraya ekleyebilirsin."
                        )
                    }
                } else {
                    items(favSurahs, key = { it.id }) { fav ->
                        val sNum = fav.surahNumber.toInt().coerceIn(1, 114)
                        val surah = QuranCatalog.surahByNumber(sNum)
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSurahReader(sNum, null) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RubElHizbBadge(numberText = sNum.toString(), sizeDp = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(surah.nameTr, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${surah.ayahCount} Ayet • ${surah.revelationTypeTr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = surah.nameAr,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                IconButton(onClick = { onPlaySurah(sNum, 1) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Dinle", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onRemoveFavorite(fav.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Favoriden kaldır")
                                }
                            }
                        }
                    }
                }
            }

            PersonalTab.FAV_AYAHS -> {
                if (favAyahs.isEmpty()) {
                    item {
                        EmptyPersonalCard(
                            icon = Icons.Rounded.Favorite,
                            title = "Henüz favori ayetin yok",
                            subtitle = "Kur’an okurken beğendiğin ayetlerin kalp simgesine dokunarak burada saklayabilirsin."
                        )
                    }
                } else {
                    items(favAyahs, key = { it.id }) { fav ->
                        val sNum = fav.surahNumber.toInt().coerceIn(1, 114)
                        val aNum = fav.ayahNumber.toInt().coerceAtLeast(1)
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSurahReader(sNum, aNum) }
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
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${fav.surahNameTr} • $aNum. Ayet",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onPlaySurah(sNum, aNum) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Ayeti dinle", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onRemoveFavorite(fav.id) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Kaldır")
                                        }
                                    }
                                }
                                if (!fav.arabicText.isNullOrBlank()) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = fav.arabicText,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 21.sp,
                                            lineHeight = 36.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (!fav.turkishText.isNullOrBlank()) {
                                    Text(
                                        text = fav.turkishText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PersonalTab.HISTORY -> {
                if (history.isEmpty()) {
                    item {
                        EmptyPersonalCard(
                            icon = Icons.Rounded.History,
                            title = "Henüz dinleme geçmişin yok",
                            subtitle = "Herhangi bir sureyi dinlemeye başladığında son dinlenenlerin burada otomatik listelenir."
                        )
                    }
                } else {
                    items(history, key = { it.id }) { item ->
                        val sNum = item.surahNumber.toInt().coerceIn(1, 114)
                        val aNum = item.ayahNumber.toInt().coerceAtLeast(1)
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaySurah(sNum, aNum) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RubElHizbBadge(numberText = sNum.toString(), sizeDp = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${item.surahNameTr} Suresi", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.reciterName} • $aNum. Ayet (${item.listenCount} kez)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onOpenSurahReader(sNum, aNum) }) {
                                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Oku", tint = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { onPlaySurah(sNum, aNum) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Dinle", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPersonalCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
