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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.CONTINENTS
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.ScoreRing
import com.miskibin.poznajswiat.ui.StatColumn

@Composable
fun HomeScreen(
    data: AppData,
    progress: Progress,
    continent: String?,
    onContinentChange: (String?) -> Unit,
    onStartQuiz: (QuizMode) -> Unit,
    onOpenLearn: () -> Unit,
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
            onClick = { onStartQuiz(QuizMode.FLAGS) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.Public,
            title = stringResource(R.string.mode_map_title),
            description = stringResource(R.string.mode_map_desc),
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { onStartQuiz(QuizMode.MAP) },
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            icon = Icons.Default.LocationCity,
            title = stringResource(R.string.mode_capitals_title),
            description = stringResource(R.string.mode_capitals_desc),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = { onStartQuiz(QuizMode.CAPITALS) },
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

@Composable
private fun StatsCard(data: AppData, progress: Progress) {
    val totalSlots = data.countries.size * QuizMode.entries.size
    val mastered = QuizMode.entries.sumOf { progress.masteredCount(it, data.countries) }
    val attempts = progress.totalAttempts
    val accuracy = if (attempts == 0) 0 else (100 * progress.totalCorrect / attempts)

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
            Spacer(Modifier.width(20.dp))
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn("$mastered", stringResource(R.string.stats_mastered))
                StatColumn("$attempts", stringResource(R.string.stats_answers))
                StatColumn("$accuracy%", stringResource(R.string.stats_accuracy))
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
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
