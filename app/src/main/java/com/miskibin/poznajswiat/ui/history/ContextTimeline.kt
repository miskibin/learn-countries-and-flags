package com.miskibin.poznajswiat.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miskibin.poznajswiat.data.EVENT_THEMES
import com.miskibin.poznajswiat.data.HistoryEvent
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Categorical palette keyed by [EVENT_THEMES] order. Each entry is a
 * (light-surface, dark-surface) pair chosen to stay legible on both the light
 * and dark backgrounds defined in the app theme. "Polska" (index 0) is a
 * deliberate crimson red so the learner's home country reads as a warm anchor.
 */
private val THEME_PALETTE: List<Pair<Color, Color>> = listOf(
    Color(0xFFD32F2F) to Color(0xFFEF5350), // Polska — crimson red
    Color(0xFFEF6C00) to Color(0xFFFFB74D), // Wojny — orange
    Color(0xFF6A1B9A) to Color(0xFFBA68C8), // Władcy i imperia — purple
    Color(0xFF00897B) to Color(0xFF4DB6AC), // Odkrycia — teal
    Color(0xFF1565C0) to Color(0xFF64B5F6), // Nauka i technika — blue
    Color(0xFF3949AB) to Color(0xFF7986CB), // Religia — indigo
    Color(0xFFE64A19) to Color(0xFFFF8A65), // Rewolucje — deep orange
    Color(0xFFC2185B) to Color(0xFFF06292), // Kultura — magenta
    Color(0xFF5D4037) to Color(0xFFA1887F), // Katastrofy — brown
    Color(0xFF2E7D32) to Color(0xFF81C784), // Społeczeństwo i prawo — green
    Color(0xFFF9A825) to Color(0xFFFFD54F), // Gospodarka — amber
)

private val UNKNOWN_THEME = Color(0xFF757575) to Color(0xFF9E9E9E)

/**
 * Stable, distinguishable color for a history theme [tag], picked from a
 * categorical palette that works on both light and dark surfaces. Unknown tags
 * fall back to a neutral gray. This is the schema anchor: a consistent
 * color→theme mapping lets learners recall the category from the hue alone.
 */
fun themeColor(tag: String, dark: Boolean): Color {
    val idx = EVENT_THEMES.indexOf(tag)
    val pair = if (idx in THEME_PALETTE.indices) THEME_PALETTE[idx] else UNKNOWN_THEME
    return if (dark) pair.second else pair.first
}

/** The related events chosen for [ContextTimeline] plus the year window to draw. */
data class TimelineContext(
    val related: List<HistoryEvent>,
    val windowStart: Int,
    val windowEnd: Int,
)

private const val MIN_SPAN = 80
private const val READABLE_SPAN = 600
private const val CLAMP_LOW = -10000
private const val CLAMP_HIGH = 2026

/**
 * Selects the events most RELATED to [target] so it can be encoded against
 * existing memories (relational encoding), and computes a readable year window.
 *
 * Scoring per candidate: +3 per shared tag, +2 per shared country, and a
 * temporal-proximity bonus (+3 if |Δyear| ≤ 50, +2 if ≤ 120, +1 if ≤ 300).
 * Candidates scoring below 2 have no meaningful relation and are dropped. The
 * top [maxRelated] survive (ties broken by smaller |Δyear|).
 *
 * The window spans target + chosen events padded by 12% of the span per side,
 * with a minimum span of [MIN_SPAN] years (centered on the target when nothing
 * relates), clamped to [CLAMP_LOW]..[CLAMP_HIGH]. To keep the axis readable, a
 * raw span past [READABLE_SPAN] years drops the farthest related events rather
 * than stretching the scale further.
 */
fun buildTimelineContext(
    target: HistoryEvent,
    all: List<HistoryEvent>,
    maxRelated: Int = 4,
): TimelineContext {
    val targetTags = target.tags.toSet()
    val targetCountries = target.countries.toSet()

    fun proximityBonus(dist: Int): Int = when {
        dist <= 50 -> 3
        dist <= 120 -> 2
        dist <= 300 -> 1
        else -> 0
    }

    data class Scored(val event: HistoryEvent, val score: Int, val dist: Int)

    val scored = all.asSequence()
        .filter { it !== target }
        .map { e ->
            val sharedTags = e.tags.count { it in targetTags }
            val sharedCountries = e.countries.count { it in targetCountries }
            val dist = abs(e.year - target.year)
            val score = sharedTags * 3 + sharedCountries * 2 + proximityBonus(dist)
            Scored(e, score, dist)
        }
        .filter { it.score >= 2 }
        .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.dist })
        .take(maxRelated)
        .toMutableList()

    // Keep the window readable: once the raw span exceeds READABLE_SPAN years,
    // drop the farthest related events instead of stretching the scale.
    while (scored.isNotEmpty()) {
        val years = scored.map { it.event.year } + target.year
        if (years.max() - years.min() <= READABLE_SPAN) break
        val farthest = scored.maxByOrNull { it.dist } ?: break
        scored.remove(farthest)
    }

    val related = scored.map { it.event }

    val years = related.map { it.year } + target.year
    var lo = years.min()
    var hi = years.max()
    val span = hi - lo
    val pad = (span * 0.12f).roundToInt()
    lo -= pad
    hi += pad

    if (hi - lo < MIN_SPAN) {
        val center = if (related.isEmpty()) target.year else (lo + hi) / 2
        lo = center - MIN_SPAN / 2
        hi = center + MIN_SPAN / 2
    }

    lo = lo.coerceIn(CLAMP_LOW, CLAMP_HIGH)
    hi = hi.coerceIn(CLAMP_LOW, CLAMP_HIGH)
    if (hi <= lo) hi = (lo + MIN_SPAN).coerceAtMost(CLAMP_HIGH)

    return TimelineContext(related, lo, hi)
}

/**
 * Polish gap label for the distance between two consecutive events, correctly
 * declining "rok/lata/lat": 1→"rok"; a units digit of 2-4 (but not the 12-14
 * teens)→"lata"; everything else→"lat". Zero-year gaps collapse to a same-year
 * note. This relational spacing turns the raw year numbers into a felt sense of
 * how far apart events sit — the magnitude anchor that makes the order stick.
 */
private fun gapLabel(diff: Int): String {
    val n = abs(diff)
    if (n == 0) return "ten sam rok"
    val word = when {
        n == 1 -> "rok"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "lata"
        else -> "lat"
    }
    return "$n $word później"
}

private const val RAIL_WIDTH_DP = 40

/** BCE labels ("480 p.n.e.") need more room than plain CE years ("1920"). */
private const val YEAR_WIDTH_CE_DP = 52
private const val YEAR_WIDTH_BCE_DP = 92

/**
 * Compact VERTICAL context chronology built for MEMORABILITY. Events (the
 * [target] merged with its [context] relatives) are laid out top-to-bottom in
 * year order as single-line rows threaded onto a continuous vertical rail. The
 * design exploits several well-studied memory principles:
 *
 * - **Spatial + relational encoding** — a top-to-bottom chronological list maps
 *   time onto a stable vertical axis, anchoring the target against neighbours
 *   the learner may already know instead of a hard-to-read horizontal scale.
 * - **Magnitude anchoring** — a tiny Polish gap label between each pair of rows
 *   ("33 lata później") names the felt distance in time, so the ordering is
 *   remembered as intervals, not just isolated dates.
 * - **Dual coding** — every row carries an emoji node on the rail, pairing a
 *   verbal label with a visual code retrievable through two independent routes.
 * - **Isolation effect (von Restorff)** — exactly one row, the target, is
 *   wrapped in a rounded primaryContainer surface with enlarged text and emoji
 *   so it pops out from its neighbours and is the item most likely to be recalled.
 * - **Schema building** — theme chips and per-row year colors reinforce a stable
 *   color→theme mapping, letting the category be recalled from hue alone.
 */
@Composable
fun ContextTimeline(
    target: HistoryEvent,
    context: TimelineContext,
    modifier: Modifier = Modifier,
    onEventClick: ((HistoryEvent) -> Unit)? = null,
) {
    val dark = isSystemInDarkTheme()
    val rail = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Merge target + related and lay them out chronologically.
    val rows = remember(target, context) {
        (listOf(target) + context.related).sortedBy { it.year }
    }

    // First-tags of the shown events, de-duplicated in encounter order.
    val chipTags = remember(rows) {
        rows.mapNotNull { it.tags.firstOrNull() }.distinct()
    }

    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val yearWidth =
                if (rows.any { it.year < 0 }) YEAR_WIDTH_BCE_DP.dp else YEAR_WIDTH_CE_DP.dp
            rows.forEachIndexed { index, event ->
                val isTarget = event === target
                val color = event.tags.firstOrNull()?.let { themeColor(it, dark) } ?: muted
                EventRow(
                    event = event,
                    isTarget = isTarget,
                    color = color,
                    rail = rail,
                    yearWidth = yearWidth,
                    onEventClick = onEventClick,
                )
                if (index < rows.lastIndex) {
                    GapConnector(
                        label = gapLabel(rows[index + 1].year - event.year),
                        rail = rail,
                        muted = muted,
                    )
                }
            }
        }

        // Theme chips — the color→theme schema legend.
        if (chipTags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                chipTags.forEach { tag ->
                    val c = themeColor(tag, dark)
                    Surface(
                        color = c.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = c,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One chronology row: rail node (emoji), colored year, then the title. */
@Composable
private fun EventRow(
    event: HistoryEvent,
    isTarget: Boolean,
    color: Color,
    rail: Color,
    yearWidth: Dp,
    onEventClick: ((HistoryEvent) -> Unit)?,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(
            if (onEventClick != null) Modifier.clickable { onEventClick(event) }
            else Modifier,
        )

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Rail(rail) {
                Text(
                    event.emoji.ifEmpty { "•" },
                    fontSize = if (isTarget) 20.sp else 15.sp,
                    maxLines = 1,
                )
            }
            Text(
                event.yearLabel,
                style = if (isTarget) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                modifier = Modifier.width(yearWidth),
            )
            Text(
                event.title,
                style = if (isTarget) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (isTarget) {
        // Von Restorff: the single highlighted row.
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = rowModifier,
        ) {
            content()
        }
    } else {
        Box(rowModifier) { content() }
    }
}

/** The short spacer between two rows: rail continues plus the Polish gap label. */
@Composable
private fun GapConnector(label: String, rail: Color, muted: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rail(rail) {}
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = muted,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * The fixed-width rail column: a continuous 2.dp vertical line running the full
 * height of the row with [node] (an emoji marker, or nothing on connectors)
 * centered on top of it.
 */
@Composable
private fun Rail(rail: Color, node: @Composable () -> Unit) {
    Box(
        modifier = Modifier.width(RAIL_WIDTH_DP.dp).fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(rail),
        )
        node()
    }
}
