package com.miskibin.poznajswiat.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.FlagImage
import com.miskibin.poznajswiat.ui.MasteryDots
import com.miskibin.poznajswiat.ui.history.EventCard
import com.miskibin.poznajswiat.ui.map.WorldMapView
import com.miskibin.poznajswiat.ui.quiz.FactCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CountryDetailScreen(
    data: AppData,
    progress: Progress,
    cca2: String,
    onOpenCountry: (String) -> Unit,
    onBack: () -> Unit,
) {
    val country = data.byCode[cca2] ?: return
    val events = data.eventsFor(cca2)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(country.namePl) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FlagImage(
                    resId = data.flagRes[country.cca2] ?: 0,
                    contentDescription = country.namePl,
                    modifier = Modifier.width(260.dp),
                    corner = 16.dp,
                )
            }
            Spacer(Modifier.height(16.dp))

            Card {
                Column(Modifier.padding(4.dp)) {
                    InfoRow(
                        Icons.Default.LocationCity,
                        stringResource(R.string.detail_capital),
                        country.capitalPl,
                    )
                    InfoRow(
                        Icons.Default.Public,
                        stringResource(R.string.detail_continent),
                        country.continent,
                    )
                    if (country.subregionPl.isNotEmpty() &&
                        country.subregionPl != country.continent
                    ) {
                        InfoRow(
                            Icons.Default.Explore,
                            stringResource(R.string.detail_region),
                            country.subregionPl,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            FactCard(fact = country.fact)
            Spacer(Modifier.height(12.dp))

            WikiSection(stringResource(R.string.detail_wiki_geo), country.wikiGeo)

            Text(
                stringResource(R.string.detail_location),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            val highlight = MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                WorldMapView(
                    map = data.worldMap,
                    markerCountries = if (country.hasPoly) emptyList() else listOf(country),
                    interactive = false,
                    focusOn = country,
                    fillFor = { code -> if (code == country.cca2) highlight else null },
                )
            }
            Spacer(Modifier.height(12.dp))

            WikiSection(stringResource(R.string.detail_wiki_hist), country.wikiHist)
            WikiSection(stringResource(R.string.detail_wiki_now), country.wikiNow)

            if (country.neighbors.isNotEmpty()) {
                Text(
                    stringResource(R.string.detail_neighbors),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    country.neighbors.forEach { code ->
                        val neighbor = data.byCode[code] ?: return@forEach
                        Card(
                            onClick = { onOpenCountry(code) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FlagImage(
                                    resId = data.flagRes[code] ?: 0,
                                    contentDescription = null,
                                    modifier = Modifier.width(26.dp),
                                    corner = 4.dp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(neighbor.namePl, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (events.isNotEmpty()) {
                Text(
                    stringResource(R.string.detail_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                events.forEach { event ->
                    EventCard(event, data, onOpenCountry = null)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(4.dp))
            }

            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.detail_progress),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    ProgressRow(
                        Icons.Default.Flag,
                        stringResource(R.string.mode_flags_title),
                        progress.streak(QuizMode.FLAGS, country.cca2),
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressRow(
                        Icons.Default.Public,
                        stringResource(R.string.mode_map_title),
                        progress.streak(QuizMode.MAP, country.cca2),
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressRow(
                        Icons.Default.LocationCity,
                        stringResource(R.string.mode_capitals_title),
                        progress.streak(QuizMode.CAPITALS, country.cca2),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WikiSection(title: String, text: String) {
    if (text.isEmpty()) return
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    Card {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProgressRow(icon: ImageVector, label: String, streak: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        MasteryDots(streak = streak)
    }
}
