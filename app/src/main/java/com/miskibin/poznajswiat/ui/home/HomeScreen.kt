package com.miskibin.poznajswiat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.CONTINENTS
import com.miskibin.poznajswiat.data.PATH_ORDER
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.ScoreRing
import com.miskibin.poznajswiat.ui.StatColumn
import com.miskibin.poznajswiat.ui.map.continentMastery
import com.miskibin.poznajswiat.ui.map.modesFor
import java.time.LocalDate

@Composable
fun HomeScreen(
    data: AppData,
    progress: Progress,
    continent: String?,
    onContinentChange: (String?) -> Unit,
    onStartSession: (String) -> Unit,
    onOpenLearn: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenKnowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        ReviewCard(progress, onStart = { onStartSession("powtorka") })
        Spacer(Modifier.height(12.dp))

        LearningPathCard(data, progress, onStartSession, onContinentChange)
        Spacer(Modifier.height(12.dp))

        StatsCard(data, progress)
        Spacer(Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = continent == null,
                    onClick = { onContinentChange(null) },
                    label = { Text(stringResource(R.string.continent_all)) },
                )
            }
            items(CONTINENTS) { c ->
                FilterChip(
                    selected = continent == c,
                    onClick = { onContinentChange(c) },
                    label = { Text(c) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        ModeCard(
            icon = Icons.Default.Flag,
            title = stringResource(R.string.mode_flags_title),
            description = stringResource(R.string.mode_flags_desc),
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = { onStartSession(QuizMode.FLAGS.id) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.Public,
            title = stringResource(R.string.mode_map_title),
            description = stringResource(R.string.mode_map_desc),
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { onStartSession(QuizMode.MAP.id) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.LocationCity,
            title = stringResource(R.string.mode_capitals_title),
            description = stringResource(R.string.mode_capitals_desc),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = { onStartSession(QuizMode.CAPITALS.id) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.HistoryEdu,
            title = stringResource(R.string.mode_history_title),
            description = stringResource(R.string.mode_history_desc),
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = { onStartSession(QuizMode.HISTORY.id) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.Timeline,
            title = stringResource(R.string.timeline_title),
            description = stringResource(R.string.timeline_desc),
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onOpenTimeline,
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.Map,
            title = stringResource(R.string.knowledge_title),
            description = stringResource(R.string.knowledge_desc),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenKnowledge,
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.mode_learn_title),
            description = stringResource(R.string.mode_learn_desc),
            container = MaterialTheme.colorScheme.surfaceVariant,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onOpenLearn,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Guided path: master continents one by one. Recommends the first continent
 * (in PATH_ORDER) that is not yet 80% mastered and starts an interleaved
 * session scoped to it.
 */
@Composable
private fun LearningPathCard(
    data: AppData,
    progress: Progress,
    onStartSession: (String) -> Unit,
    onContinentChange: (String?) -> Unit,
) {
    val fractions = remember(progress, data) {
        PATH_ORDER.associateWith { continent ->
            continentMastery(progress, data.countries.filter { it.continent == continent })
        }
    }
    val recommended = PATH_ORDER.firstOrNull { (fractions[it] ?: 0f) < 0.8f } ?: PATH_ORDER.last()

    Card(colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.path_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    onContinentChange(recommended)
                    onStartSession("mix")
                }) {
                    Text(stringResource(R.string.path_learn, recommended))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(PATH_ORDER) { continent ->
                    val fraction = fractions[continent] ?: 0f
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(
                            fraction = fraction,
                            modifier = Modifier.size(46.dp),
                            ringWidth = 5.dp,
                            color = if (continent == recommended) {
                                MaterialTheme.colorScheme.primary
                            } else Color(0xFF43A047),
                        ) {
                            Text(
                                "${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            continent.replace("Ameryka ", "Am. "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (continent == recommended) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(progress: Progress, onStart: () -> Unit) {
    val today = remember { LocalDate.now().toEpochDay() }
    val dueCount = remember(progress) { progress.dueItems(today).size }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Replay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.review_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = if (dueCount > 0) {
                        pluralStringResource(R.plurals.review_due, dueCount, dueCount)
                    } else {
                        stringResource(R.string.review_none_due)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.review_start))
            }
        }
    }
}

@Composable
private fun StatsCard(data: AppData, progress: Progress) {
    val totalSlots = data.countries.sumOf { modesFor(it).size } + data.events.size
    val mastered = data.countries.sumOf { c ->
        modesFor(c).count { progress.isMastered(it, c.cca2) }
    } + progress.masteredCount(QuizMode.HISTORY, data.events.map { it.id.toString() })
    val attempts = progress.totalAttempts
    val accuracy = if (attempts == 0) 0 else (100 * progress.totalCorrect / attempts)
    val today = remember { LocalDate.now().toEpochDay() }
    val dayStreak = progress.currentDayStreak(today)

    Card(colors = CardDefaults.cardColors()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreRing(
                fraction = if (totalSlots == 0) 0f else mastered.toFloat() / totalSlots,
                modifier = Modifier.size(84.dp),
            ) {
                Text(
                    "${if (totalSlots == 0) 0 else 100 * mastered / totalSlots}%",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.width(16.dp))
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn("$mastered", stringResource(R.string.stats_mastered))
                StatColumn("$accuracy%", stringResource(R.string.stats_accuracy))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (dayStreak > 0) Color(0xFFE65100)
                            else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp),
                        )
                        Text("$dayStreak", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        stringResource(R.string.stats_day_streak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(onContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = onContainer)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}
