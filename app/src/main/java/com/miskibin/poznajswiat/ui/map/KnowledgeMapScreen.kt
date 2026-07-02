package com.miskibin.poznajswiat.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Country
import com.miskibin.poznajswiat.data.PATH_ORDER
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.QuizMode

/** Modes that apply to a given country (map only when it has a polygon). */
fun modesFor(country: Country): List<QuizMode> = buildList {
    add(QuizMode.FLAGS)
    add(QuizMode.CAPITALS)
    if (country.hasPoly) add(QuizMode.MAP)
}

/** 0 = untouched, 1 = started, 2 = fully mastered. */
fun knowledgeLevel(progress: Progress, country: Country): Int {
    val modes = modesFor(country)
    val attempted = modes.any { (progress.attempts[com.miskibin.poznajswiat.data.itemId(it, country.cca2)] ?: 0) > 0 }
    if (!attempted) return 0
    return if (modes.all { progress.isMastered(it, country.cca2) }) 2 else 1
}

/** Fraction of (country, mode) pairs of a continent that are mastered. */
fun continentMastery(progress: Progress, countries: List<Country>): Float {
    var total = 0
    var done = 0
    for (c in countries) {
        for (m in modesFor(c)) {
            total++
            if (progress.isMastered(m, c.cca2)) done++
        }
    }
    return if (total == 0) 0f else done.toFloat() / total
}

class KnowledgeColors(val started: Color, val mastered: Color)

@Composable
fun rememberKnowledgeColors(): KnowledgeColors {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) KnowledgeColors(Color(0xFFC8A036), Color(0xFF4CAF50))
        else KnowledgeColors(Color(0xFFF5C542), Color(0xFF43A047))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeMapScreen(
    data: AppData,
    progress: Progress,
    onOpenCountry: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = rememberKnowledgeColors()
    val levels = remember(progress, data) {
        data.countries.associate { it.cca2 to knowledgeLevel(progress, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knowledge_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp),
            ) {
                WorldMapView(
                    map = data.worldMap,
                    markerCountries = data.countries.filter { !it.hasPoly },
                    interactive = true,
                    fillFor = { code ->
                        when (levels[code]) {
                            2 -> colors.mastered
                            1 -> colors.started
                            else -> null
                        }
                    },
                    onTap = { code -> if (code != null) onOpenCountry(code) },
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LegendDot(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.legend_new))
                LegendDot(colors.started, stringResource(R.string.legend_started))
                LegendDot(colors.mastered, stringResource(R.string.legend_mastered))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                PATH_ORDER.forEach { continent ->
                    val countries = data.countries.filter { it.continent == continent }
                    val fraction = continentMastery(progress, countries)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            continent,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(150.dp),
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            color = if (fraction >= 1f) colors.mastered else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${(fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.knowledge_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
