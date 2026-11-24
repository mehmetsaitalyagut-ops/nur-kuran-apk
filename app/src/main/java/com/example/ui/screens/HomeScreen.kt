package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.DailyIbadatDoc
import com.example.data.firebase.FavoriteItemDoc
import com.example.data.firebase.ListeningHistoryDoc
import com.example.data.firebase.UserDataRepository
import com.example.data.firebase.UserPreferencesDoc
import com.example.data.local.LastListenedProgress
import com.example.model.DailyVerse
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.model.SurahInfo
import com.example.ui.UiState
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.RubElHizbBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuranArabicTextStyle
import com.example.ui.theme.QuranBismillahTextStyle

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    dailyVerse: DailyVerse,
    lastProgress: LastListenedProgress,
    preferencesState: UiState<UserPreferencesDoc>,
    historyState: UiState<List<ListeningHistoryDoc>>,
    favoritesState: UiState<List<FavoriteItemDoc>>,
    dailyIbadatState: UiState<List<DailyIbadatDoc>>,
    activeReciter: Reciter,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onSignOut: () -> Unit,
    onResumeLastListened: () -> Unit,
    onPlaySurah: (Int, Int) -> Unit,
    onOpenSurahReader: (Int, Int?) -> Unit,
    onSelectReciter: (Reciter) -> Unit,
    onToggleFavoriteSurah: (SurahInfo) -> Unit,
    onViewAllSurahs: () -> Unit,
    onOpenIbadat: () -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp
) {
    val prefs = (preferencesState as? UiState.Success)?.data
    val history = (historyState as? UiState.Success)?.data.orEmpty()
    val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()
    val ibadatList = (dailyIbadatState as? UiState.Success)?.data.orEmpty()

    val todayKey = UserDataRepository.todayKey()
    val todayRecord = ibadatList.firstOrNull { it.dateKey == todayKey }
    val readGoal = (prefs?.dailyReadGoalAyahs ?: 20L).coerceAtLeast(1L)
    val listenGoalMin = (prefs?.dailyListenGoalMinutes ?: 15L).coerceAtLeast(1L)
    val readToday = todayRecord?.ayahsRead ?: 0L
    val listenMinutesToday = (todayRecord?.listenedSeconds ?: 0L) / 60L
    val completedJuzCount = prefs?.completedJuzs?.size ?: 0

    val lastSurahNum = lastProgress.surahNumber.coerceIn(1, 114)
    val lastSurahInfo = QuranCatalog.surahByNumber(lastSurahNum)
    val lastAyahNum = lastProgress.ayahNumber.coerceIn(1, lastSurahInfo.ayahCount)
    val lastReciter = QuranCatalog.reciterById(lastProgress.reciterId)
    val lastHistory = history.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nur_user_logo_1795507125149),
                        contentDescription = "NUR logosu",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    Column {
                        Text(
                            text = "NUR",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kur’an-ı Kerim ve İbadet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("home_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("home_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Çıkış Yap",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Bismillahirrahmanirrahim Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_mosque_banner_1795505668700),
                    contentDescription = "Bismillahirrahmanirrahim İslami mimari görseli",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xAA04140E),
                                    Color(0xCC061E16)
                                )
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = stringResource(R.string.bismillah_ar),
                                style = QuranBismillahTextStyle.copy(fontSize = 28.sp),
                                color = Color(0xFFE6C15A),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bismillahirrahmanirrahim",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF6F1E7)
                        )
                        Text(
                            text = stringResource(R.string.bismillah_tr),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCFC6B5),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Kaldığın Yerden Devam Et & Son Dinlenen Sure
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResumeLastListened() }
                        .testTag("home_resume_card"),
                    highlightBorder = true
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdded,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kaldığın Yerden Devam Et",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$lastAyahNum. Ayet / ${lastSurahInfo.ayahCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RubElHizbBadge(
                                numberText = lastSurahInfo.number.toString(),
                                sizeDp = 46.dp,
                                isHighlighted = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lastSurahInfo.nameTr} Suresi",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$lastAyahNum. Ayet • ${lastReciter.nameTr}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = lastSurahInfo.nameAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        val surahProgress = (lastAyahNum.toFloat() / lastSurahInfo.ayahCount.coerceAtLeast(1).toFloat())
                            .coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { surahProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onResumeLastListened,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("home_resume_continue_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Devam Et", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = { onOpenSurahReader(lastSurahInfo.number, lastAyahNum) },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("home_resume_read_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Oku", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                if (lastHistory != null) {
                    val recentSurahNum = lastHistory.surahNumber.toInt().coerceIn(1, 114)
                    val recentSurah = QuranCatalog.surahByNumber(recentSurahNum)
                    val recentAyah = lastHistory.ayahNumber.toInt().coerceIn(1, recentSurah.ayahCount)
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySurah(recentSurah.number, recentAyah) }
                            .testTag("home_recent_listened_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Son Dinlenen • ${recentSurah.nameTr} Suresi ($recentAyah. Ayet)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = lastHistory.reciterName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onPlaySurah(recentSurah.number, recentAyah) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dinle", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 4. Günün Ayeti
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                highlightBorder = true
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GÜNÜN AYETİ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${dailyVerse.surahNameTr} • ${dailyVerse.ayahNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = dailyVerse.arabicText,
                            style = QuranArabicTextStyle.copy(fontSize = 23.sp, lineHeight = 40.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "“${dailyVerse.turkishText}”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlaySurah(dailyVerse.surahNumber, dailyVerse.ayahNumber) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("daily_verse_play")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ayeti Dinle")
                        }
                        OutlinedButton(
                            onClick = { onOpenSurahReader(dailyVerse.surahNumber, dailyVerse.ayahNumber) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("daily_verse_read")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Surede Gör")
                        }
                    }
                }
            }
        }

        // 5. Günlük Hedef ve İlerleme
        item {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenIbadat)
                    .testTag("home_daily_goal_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Günlük Hedef ve Hatim",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Detaylar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    val readProgress = (readToday.toFloat() / readGoal.toFloat()).coerceIn(0f, 1f)
                    val listenProgress = (listenMinutesToday.toFloat() / listenGoalMin.toFloat()).coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Okuma Hedefi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$readToday / $readGoal Ayet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        LinearProgressIndicator(
                            progress = { readProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dinleme Hedefi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$listenMinutesToday / $listenGoalMin Dk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { listenProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }

                    Text(
                        text = "Hatim İlerlemesi: $completedJuzCount / 30 Cüz (%${(completedJuzCount * 100) / 30})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 6. Hafız Seçimi
        item {
            SectionHeader(
                title = "Hafız Seçimi",
                subtitle = "Seçilen hafız tüm surelerde aktif olur"
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(QuranCatalog.reciters, key = { it.id }) { reciter ->
                    val isSelected = reciter.id == activeReciter.id
                    GlassmorphicCard(
                        modifier = Modifier
                            .width(196.dp)
                            .clickable { onSelectReciter(reciter) }
                            .testTag("home_reciter_${reciter.id}"),
                        highlightBorder = isSelected
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
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reciter.initials,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Seçili hafız",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Text(
                                text = reciter.nameTr,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = reciter.titleTr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = reciter.styleTr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 7. Popüler Sureler
        item {
            SectionHeader(
                title = "Popüler Sureler",
                actionLabel = "Tümü (114)",
                onAction = onViewAllSurahs
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(QuranCatalog.popularSurahs, key = { it.number }) { surah ->
                    val isFav = favorites.any { it.kind == "surah" && it.surahNumber.toInt() == surah.number }
                    GlassmorphicCard(
                        modifier = Modifier
                            .width(172.dp)
                            .clickable { onOpenSurahReader(surah.number, null) }
                            .testTag("popular_surah_${surah.number}")
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
                                RubElHizbBadge(numberText = surah.number.toString(), sizeDp = 36.dp)
                                IconButton(
                                    onClick = { onToggleFavoriteSurah(surah) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = stringResource(R.string.cd_favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = surah.nameAr,
                                fontFamily = AmiriFontFamily,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = surah.nameTr,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${surah.ayahCount} Ayet • ${surah.revelationTypeTr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { onPlaySurah(surah.number, 1) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dinle", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 8. Tüm Sureler Hızlı Erişim Listesi
        item {
            SectionHeader(
                title = "Tüm Sureler",
                actionLabel = "114 Sureyi Gör",
                onAction = onViewAllSurahs
            )
        }

        items(QuranCatalog.surahs.take(10), key = { "home_all_${it.number}" }) { surah ->
            val isFav = favorites.any { it.kind == "surah" && it.surahNumber.toInt() == surah.number }
            SurahRowCard(
                surah = surah,
                isFavorite = isFav,
                isPlayingThis = false,
                onOpenReader = { onOpenSurahReader(surah.number, null) },
                onPlay = { onPlaySurah(surah.number, 1) },
                onToggleFavorite = { onToggleFavoriteSurah(surah) }
            )
        }

        item {
            OutlinedButton(
                onClick = onViewAllSurahs,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_see_all_surahs")
            ) {
                Text("114 Surenin Tamamını Görüntüle")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SurahRowCard(
    surah: SurahInfo,
    isFavorite: Boolean,
    isPlayingThis: Boolean,
    onOpenReader: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenReader)
            .testTag("surah_card_${surah.number}"),
        cornerRadius = 16.dp,
        highlightBorder = isPlayingThis
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RubElHizbBadge(
                numberText = surah.number.toString(),
                sizeDp = 42.dp,
                isHighlighted = isPlayingThis
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameTr,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${surah.meaningTr} • ${surah.ayahCount} Ayet • ${surah.revelationTypeTr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = surah.nameAr,
                fontFamily = AmiriFontFamily,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("surah_fav_${surah.number}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onPlay,
                modifier = Modifier.testTag("surah_play_${surah.number}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "${surah.nameTr} suresini dinle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
