package com.miskibin.poznajswiat.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miskibin.poznajswiat.data.HistoryEvent

private val ERA_BOUNDS = intArrayOf(-10000, 477, 1492, 1914, 2026)
private val ERA_LABELS = listOf("Starożytność", "Średniowiecze", "Nowożytność", "XX–XXI w.")

/**
 * Piecewise position on the timeline: each era gets an equal quarter of the
 * width, so antiquity does not crush the last five centuries into a pixel.
 */
private fun fractionOf(year: Int): Float {
    val y = year.coerceIn(ERA_BOUNDS.first(), ERA_BOUNDS.last())
    for (i in 0 until ERA_BOUNDS.size - 1) {
        if (y <= ERA_BOUNDS[i + 1]) {
            val within = (y - ERA_BOUNDS[i]).toFloat() / (ERA_BOUNDS[i + 1] - ERA_BOUNDS[i])
            return (i + within) / (ERA_BOUNDS.size - 1)
        }
    }
    return 1f
}

/**
 * Compact "where in history is this?" visualisation shown after answering a
 * history question: era axis with the event marked, plus the nearest earlier
 * and later events from the dataset to anchor it.
 */
@Composable
fun TimelineBar(
    event: HistoryEvent,
    allEvents: List<HistoryEvent>,
    modifier: Modifier = Modifier,
) {
    val fraction = remember(event) { fractionOf(event.year) }
    val earlier = remember(event, allEvents) {
        allEvents.lastOrNull { it.year < event.year || (it.year == event.year && it.id < event.id) }
    }
    val later = remember(event, allEvents) {
        allEvents.firstOrNull { it.year > event.year || (it.year == event.year && it.id > event.id) }
    }
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelWidth = 84.dp
            val x = (maxWidth - labelWidth) * fraction
            Text(
                event.yearLabel,
                style = MaterialTheme.typography.labelLarge,
                color = primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(labelWidth)
                    .offset(x = x),
            )
        }
        Spacer(Modifier.height(2.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            val y = size.height / 2f
            drawLine(track, Offset(0f, y), Offset(size.width, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            // era boundary ticks
            for (i in 1 until ERA_BOUNDS.size - 1) {
                val tx = size.width * i / (ERA_BOUNDS.size - 1).toFloat()
                drawLine(
                    track,
                    Offset(tx, y - 5.dp.toPx()),
                    Offset(tx, y + 5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            val cx = size.width * fraction
            drawCircle(primary, radius = 7.dp.toPx(), center = Offset(cx, y))
        }
        Row(Modifier.fillMaxWidth()) {
            ERA_LABELS.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (earlier != null || later != null) {
            Spacer(Modifier.height(8.dp))
            earlier?.let {
                NeighborEventLine("◂ ${it.yearLabel} — ${it.title}")
            }
            later?.let {
                NeighborEventLine("▸ ${it.yearLabel} — ${it.title}")
            }
        }
    }
}

@Composable
private fun NeighborEventLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
