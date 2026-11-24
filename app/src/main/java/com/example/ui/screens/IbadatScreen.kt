package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.firebase.DailyIbadatDoc
import com.example.data.firebase.UserDataRepository
import com.example.data.firebase.UserPreferencesDoc
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.ui.UiState
import com.example.ui.components.GlassmorphicCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IbadatScreen(
    isDarkTheme: Boolean,
    preferencesState: UiState<UserPreferencesDoc>,
    dailyIbadatState: UiState<List<DailyIbadatDoc>>,
    activeReciter: Reciter,
    onToggleJuz: (Int) -> Unit,
    onUpdateGoals: (Long, Long) -> Unit,
    onOpenJuzInReader: (Int, Int) -> Unit,
    onSelectReciter: (Reciter) -> Unit,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
    bottomContentPadding: Dp
) {
    val prefs = (preferencesState as? UiState.Success)?.data
    val ibadatDocs = (dailyIbadatState as? UiState.Success)?.data.orEmpty()

    val readGoal = (prefs?.dailyReadGoalAyahs ?: 20L).coerceAtLeast(1L)
    val listenGoalMin = (prefs?.dailyListenGoalMinutes ?: 15L).coerceAtLeast(1L)
    val completedJuzs = prefs?.completedJuzs.orEmpty().map { it.toInt() }.toSet()
    val hatimPercent = (completedJuzs.size * 100) / 30

    val todayKey = UserDataRepository.todayKey()
    val todayDoc = ibadatDocs.firstOrNull { it.dateKey == todayKey }
    val readToday = todayDoc?.ayahsRead ?: 0L
    val listenMinToday = (todayDoc?.listenedSeconds ?: 0L) / 60L

    var showGoalDialog by remember { mutableStateOf(false) }

    val last14Days = remember {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("dd", Locale.getDefault())
        val monthFmt = SimpleDateFormat("EEE", Locale.forLanguageTag("tr"))
        (13 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            Triple(
                keyFmt.format(cal.time),
                dayFmt.format(cal.time),
                monthFmt.format(cal.time)
            )
        }
    }

    if (showGoalDialog) {
        GoalEditDialog(
            initialReadGoal = readGoal,
            initialListenGoal = listenGoalMin,
            onDismiss = { showGoalDialog = false },
            onSave = { r, l ->
                onUpdateGoals(r, l)
                showGoalDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Günlük İbadet & Hatim Takibi",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. Daily Goals Progress Card
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Günlük Hedefler",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = { showGoalDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("ibadat_edit_goals_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Düzenle", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    val readFrac = (readToday.toFloat() / readGoal.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Günlük Okuma Hedefi", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$readToday / $readGoal Ayet (%${(readFrac * 100).toInt()})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { readFrac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }

                    val listenFrac = (listenMinToday.toFloat() / listenGoalMin.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Günlük Dinleme Hedefi", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$listenMinToday / $listenGoalMin Dk (%${(listenFrac * 100).toInt()})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { listenFrac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // 2. Hatim Tracker (30 Juzs)
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hatim Takip Sistemi", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Okuduğunuz cüzleri işaretleyin, uzun basarak cüz başlangıcını okuyun",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "%$hatimPercent",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { completedJuzs.size / 30f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "${completedJuzs.size} / 30 Cüz Tamamlandı",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuranCatalog.juzList.forEach { juz ->
                            val isDone = completedJuzs.contains(juz.number)
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onToggleJuz(juz.number) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                    .testTag("hatim_juz_${juz.number}")
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = juz.titleTr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Tamamlandı",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = juz.rangeTr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onOpenJuzInReader(juz.startSurah, juz.startAyah) }
                                            .padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = "Cüzü aç",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Oku",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Daily Ibadat Calendar (Last 14 Days)
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Günlük İbadet Takvimi (Son 14 Gün)", style = MaterialTheme.typography.titleMedium)
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        last14Days.forEach { (dateKey, dayNum, dayName) ->
                            val doc = ibadatDocs.firstOrNull { it.dateKey == dateKey }
                            val hasActivity = (doc?.ayahsRead ?: 0L) > 0L || (doc?.listenedSeconds ?: 0L) > 0L
                            val goalMet = doc?.completedDailyGoal == true
                            val isToday = dateKey == todayKey

                            Column(
                                modifier = Modifier
                                    .width(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            goalMet -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                            hasActivity -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 1.5.dp else 1.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dayNum,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (goalMet) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                                if (goalMet) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Hedef tamam",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Reciter Selection List
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Varsayılan Hafız", style = MaterialTheme.typography.titleMedium)
                    QuranCatalog.reciters.forEach { reciter ->
                        val isSelected = reciter.id == activeReciter.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectReciter(reciter) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = reciter.initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reciter.nameTr, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    reciter.styleTr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Seçili",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Theme & Account Settings
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
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
                                imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Koyu Tema", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (isDarkTheme) "Siyah, zümrüt yeşili ve altın" else "Krem parşömen ve cami yeşili",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            modifier = Modifier.testTag("ibadat_theme_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ibadat_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Oturumu Kapat")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalEditDialog(
    initialReadGoal: Long,
    initialListenGoal: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    var readText by remember { mutableStateOf(initialReadGoal.toString()) }
    var listenText by remember { mutableStateOf(initialListenGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Günlük İbadet Hedefleri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = readText,
                    onValueChange = { readText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Günlük Ayet Okuma Hedefi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = listenText,
                    onValueChange = { listenText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Günlük Dinleme Hedefi (Dakika)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = readText.toLongOrNull()?.coerceIn(1L, 1000L) ?: initialReadGoal
                    val l = listenText.toLongOrNull()?.coerceIn(1L, 600L) ?: initialListenGoal
                    onSave(r, l)
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
