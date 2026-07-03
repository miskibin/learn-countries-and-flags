package com.miskibin.poznajswiat.ui.history

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    focusEventId: Int = -1,
) {
    val grouped: List<Pair<String, List<HistoryEvent>>> = remember(data) {
        data.events.groupBy { eraName(it.year) }.toList()
    }
    val listState = rememberLazyListState()
    // Flat item order mirrors the LazyColumn content (header + events per era)
    // so we can scroll straight to a focused event.
    val focusIndex = remember(grouped, focusEventId) {
        if (focusEventId < 0) -1
        else {
            var idx = 0
            var found = -1
            for ((_, events) in grouped) {
                idx++ // era header
                for (event in events) {
                    if (event.id == focusEventId) found = idx
                    idx++
                }
            }
            found
        }
    }
    LaunchedEffect(focusIndex) {
        if (focusIndex >= 0) listState.scrollToItem((focusIndex - 1).coerceAtLeast(0))
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
            state = listState,
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
                    EventCard(
                        event,
                        data,
                        onOpenCountry,
                        highlighted = event.id == focusEventId,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * Timeline card: header row = year + flags, then emoji+title, then the
 * description. Tapping the card expands the mini-wiki (background and
 * consequences) when available.
 */
@Composable
fun EventCard(
    event: HistoryEvent,
    data: AppData,
    onOpenCountry: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val accent = event.tags.firstOrNull()?.let { themeColor(it, dark) }
        ?: MaterialTheme.colorScheme.primary
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }

    Card(
        modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp),
                    )
                } else Modifier
            )
            .then(
                if (event.wiki.isNotEmpty()) {
                    Modifier.clickable { expanded = !expanded }
                } else Modifier
            ),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header: year chip on the left, flags on the right.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                Spacer(Modifier.weight(1f))
                event.countries.forEach { code ->
                    FlagImage(
                        resId = data.flagRes[code] ?: 0,
                        contentDescription = data.byCode[code]?.namePl,
                        modifier = Modifier
                            .width(34.dp)
                            .padding(start = 6.dp)
                            .then(
                                if (onOpenCountry != null) {
                                    Modifier.clickable { onOpenCountry(code) }
                                } else Modifier
                            ),
                        corner = 4.dp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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
            if (event.wiki.isNotEmpty()) {
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            event.wiki,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        if (expanded) R.string.event_collapse else R.string.event_expand
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
