package com.miskibin.poznajswiat.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.HistoryEvent
import com.miskibin.poznajswiat.ui.FlagImage

fun eraName(year: Int): String = when {
    year < 477 -> "Starożytność"
    year < 1492 -> "Średniowiecze"
    year < 1914 -> "Nowożytność"
    else -> "XX i XXI wiek"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    data: AppData,
    onOpenCountry: (String) -> Unit,
    onBack: () -> Unit,
) {
    val grouped: List<Pair<String, List<HistoryEvent>>> = remember(data) {
        data.events.groupBy { eraName(it.year) }.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timeline_title)) },
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
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            grouped.forEach { (era, events) ->
                item(key = era) {
                    Text(
                        era,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                items(events, key = { it.id }) { event ->
                    EventCard(event, data, onOpenCountry)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: HistoryEvent,
    data: AppData,
    onOpenCountry: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val accent = event.tags.firstOrNull()?.let { themeColor(it, dark) }
        ?: MaterialTheme.colorScheme.primary
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp)) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    event.yearLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (event.emoji.isNotEmpty()) "${event.emoji} ${event.title}" else event.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    event.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (event.countries.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        event.countries.forEach { code ->
                            val country = data.byCode[code] ?: return@forEach
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = if (onOpenCountry != null) {
                                    Modifier.clickable { onOpenCountry(code) }
                                } else Modifier,
                            ) {
                                FlagImage(
                                    resId = data.flagRes[code] ?: 0,
                                    contentDescription = country.namePl,
                                    modifier = Modifier.width(28.dp),
                                    corner = 4.dp,
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    country.namePl,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
