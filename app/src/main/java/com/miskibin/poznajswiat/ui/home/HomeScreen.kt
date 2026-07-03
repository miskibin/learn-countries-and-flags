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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.CONTINENTS
import com.miskibin.poznajswiat.data.PATH_ORDER
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.data.SESSION_MIX
import com.miskibin.poznajswiat.data.SESSION_REVIEW
import com.miskibin.poznajswiat.ui.ScoreRing
import com.miskibin.poznajswiat.ui.map.continentMastery
import com.miskibin.poznajswiat.ui.map.modesFor
import java.time.LocalDate

/**
 * Home: one obvious primary action (the hero card), a slim stats strip,
 * a 2x2 practice grid and a short "explore" list — visual hierarchy over
 * a wall of same-looking cards.
 */
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
        Spacer(Modifier.height(14.dp))

        HeroCard(data, progress, onContinentChange, onStartSession)
        Spacer(Modifier.height(10.dp))

        StatsStrip(data, progress)
        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.home_practice),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeTile(
                Icons.Default.Flag,
                stringResource(R.string.mode_flags_title),
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Modifier.weight(1f),
            ) { onStartSession(QuizMode.FLAGS.id) }
            PracticeTile(
                Icons.Default.Public,
                stringResource(R.string.mode_map_title),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                Modifier.weight(1f),
            ) { onStartSession(QuizMode.MAP.id) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeTile(
                Icons.Default.LocationCity,
                stringResource(R.string.mode_capitals_title),
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Modifier.weight(1f),
            ) { onStartSession(QuizMode.CAPITALS.id) }
            PracticeTile(
                Icons.Default.HistoryEdu,
                stringResource(R.string.mode_history_title),
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Modifier.weight(1f),
            ) { onStartSession(QuizMode.HISTORY.id) }
        }
        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.home_explore),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ExploreRow(Icons.Default.Timeline, stringResource(R.string.timeline_title), onOpenTimeline)
        ExploreRow(Icons.Default.Map, stringResource(R.string.knowledge_title), onOpenKnowledge)
        ExploreRow(
            Icons.AutoMirrored.Filled.MenuBook,
            stringResource(R.string.mode_learn_title),
            onOpenLearn,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The single primary CTA: overdue reviews win; otherwise continue the
 * learning path on the recommended continent.
 */
@Composable
private fun HeroCard(
    data: AppData,
    progress: Progress,
    onContinentChange: (String?) -> Unit,
    onStartSession: (String) -> Unit,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val dueCount = remember(progress) { progress.dueItems(today).size }
    val fractions = remember(progress, data) {
        PATH_ORDER.associateWith { continent ->
            continentMastery(progress, data.countries.filter { it.continent == continent })
        }
    }
    val recommended = PATH_ORDER.firstOrNull { (fractions[it] ?: 0f) < 0.8f } ?: PATH_ORDER.last()
    val reviewMode = dueCount > 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.home_continue),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = if (reviewMode) {
                    pluralStringResource(R.plurals.review_due, dueCount, dueCount)
                } else {
                    stringResource(R.string.hero_path_desc, recommended)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PATH_ORDER.take(3).forEach { continent ->
                        val fraction = fractions[continent] ?: 0f
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ScoreRing(
                                fraction = fraction,
                                modifier = Modifier.size(34.dp),
                                ringWidth = 4.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                track = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            ) {}
                            Spacer(Modifier.height(2.dp))
                            Text(
                                continent.replace("Ameryka ", "Am. "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        if (reviewMode) {
                            onStartSession(SESSION_REVIEW)
                        } else {
                            onContinentChange(recommended)
                            onStartSession(SESSION_MIX)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.testTag("hero_action"),
                ) {
                    Text(
                        stringResource(
                            if (reviewMode) R.string.hero_review_button
                            else R.string.hero_path_button
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsStrip(data: AppData, progress: Progress) {
    val totalSlots = data.countries.sumOf { modesFor(it).size } + data.events.size
    val mastered = data.countries.sumOf { c ->
        modesFor(c).count { progress.isMastered(it, c.cca2) }
    } + progress.masteredCount(QuizMode.HISTORY, data.events.map { it.id.toString() })
    val today = remember { LocalDate.now().toEpochDay() }
    val dayStreak = progress.currentDayStreak(today)

    Card(colors = CardDefaults.cardColors()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.stats_level, progress.level),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            LinearProgressIndicator(
                progress = { progress.levelFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = stringResource(R.string.stats_day_streak),
                tint = if (dayStreak > 0) Color(0xFFE65100)
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
            Text("$dayStreak", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.stats_mastered_short, mastered, totalSlots),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PracticeTile(
    icon: ImageVector,
    title: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExploreRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
