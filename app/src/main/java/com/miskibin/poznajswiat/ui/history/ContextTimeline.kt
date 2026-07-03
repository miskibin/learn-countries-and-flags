package com.miskibin.poznajswiat.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/** "44 p.n.e." / "1939" for an arbitrary year. */
private fun yearLabel(year: Int): String = if (year < 0) "${-year} p.n.e." else "$year"

/** One resolved related marker: its screen position, side and label row. */
private data class Marker(
    val event: HistoryEvent,
    val fraction: Float,
    val above: Boolean,
    val row: Int,
    val color: Color,
)

private val BOX_HEIGHT = 132.dp
private val AXIS_Y = 64.dp
private val TARGET_LABEL_W = 78.dp
private val RELATED_LABEL_W = 66.dp

/**
 * Compact context timeline built for MEMORABILITY. It exploits several
 * well-studied memory principles:
 *
 * - **Dual coding** — every event carries an emoji, pairing a verbal label with
 *   a visual code so it can be retrieved through two independent routes.
 * - **Relational encoding** — the [context]'s related events anchor the target
 *   to things the learner may already know, on a single linear year scale.
 * - **Isolation effect (von Restorff)** — the target is enlarged and wrapped in
 *   a soft primary-colored halo so it pops out from its neighbours and is the
 *   item most likely to be remembered.
 * - **Schema building** — theme chips and per-marker colors reinforce a stable
 *   color→theme mapping, letting the category be recalled from hue alone.
 *
 * Positioning mirrors [TimelineBar]: a [BoxWithConstraints] with `offset(x)`
 * placement, but the scale here is LINEAR across [TimelineContext.windowStart]..
 * [TimelineContext.windowEnd] rather than piecewise by era.
 */
@Composable
fun ContextTimeline(
    target: HistoryEvent,
    context: TimelineContext,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val start = context.windowStart
    val end = context.windowEnd
    val span = (end - start).coerceAtLeast(1)

    fun fractionOf(year: Int): Float =
        ((year - start).toFloat() / span).coerceIn(0f, 1f)

    val targetFraction = remember(target, start, end) { fractionOf(target.year) }

    // Resolve each related event to a side and label row. Alternate above/below
    // to reduce collisions, then bump close neighbours (< 10% of width) onto a
    // second row on the same side — at most two rows per side.
    val markers = remember(context, dark) {
        val used = mutableListOf<Marker>()
        context.related.forEachIndexed { index, e ->
            val frac = fractionOf(e.year)
            val above = index % 2 == 0
            val sameSide = used.filter { it.above == above }
            var row = 0
            while (row < 2 && sameSide.any { it.row == row && abs(it.fraction - frac) < 0.10f }) {
                row++
            }
            if (row > 1) row = 1
            val color = e.tags.firstOrNull()?.let { themeColor(it, dark) } ?: muted
            used += Marker(e, frac, above, row, color)
        }
        used
    }

    // First-tags of target + related, de-duplicated in encounter order.
    val chipTags = remember(target, context) {
        (target.tags.take(1) + context.related.mapNotNull { it.tags.firstOrNull() })
            .distinct()
    }

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(BOX_HEIGHT),
        ) {
            val w = maxWidth

            fun clampedX(fraction: Float, labelW: Dp): Dp =
                (w * fraction - labelW / 2).coerceIn(0.dp, (w - labelW).coerceAtLeast(0.dp))

            // Axis, subtle ticks and the target's isolation halo (drawn behind).
            Canvas(Modifier.fillMaxWidth().height(BOX_HEIGHT)) {
                val y = AXIS_Y.toPx()
                drawLine(
                    track,
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val ticks = 8
                for (i in 0..ticks) {
                    val tx = size.width * i / ticks
                    drawLine(
                        track,
                        Offset(tx, y - 3.dp.toPx()),
                        Offset(tx, y + 3.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                // Von Restorff halo behind the target emoji.
                drawCircle(
                    primary.copy(alpha = 0.15f),
                    radius = 20.dp.toPx(),
                    center = Offset(size.width * targetFraction, y),
                )
            }

            // Corner year labels.
            Text(
                yearLabel(start),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = muted,
                modifier = Modifier.align(Alignment.TopStart).offset(y = AXIS_Y + 6.dp),
            )
            Text(
                yearLabel(end),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = muted,
                modifier = Modifier.align(Alignment.TopEnd).offset(y = AXIS_Y + 6.dp),
            )

            // Related markers.
            markers.forEach { m ->
                val yOffset = if (m.above) {
                    AXIS_Y - 44.dp - 14.dp * m.row
                } else {
                    AXIS_Y + 8.dp + 14.dp * m.row
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(RELATED_LABEL_W)
                        .offset(x = clampedX(m.fraction, RELATED_LABEL_W), y = yOffset),
                ) {
                    if (m.above) {
                        RelatedTitle(m.event.title, m.color)
                        RelatedYear(m.event.yearLabel, m.color)
                        RelatedEmoji(m.event.emoji)
                    } else {
                        RelatedEmoji(m.event.emoji)
                        RelatedYear(m.event.yearLabel, m.color)
                        RelatedTitle(m.event.title, m.color)
                    }
                }
            }

            // Target — enlarged, bold year above, title below, halo behind.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(TARGET_LABEL_W)
                    .offset(x = clampedX(targetFraction, TARGET_LABEL_W), y = AXIS_Y - 30.dp),
            ) {
                Text(
                    target.yearLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = primary,
                    maxLines = 1,
                )
                Text(target.emoji.ifEmpty { "📍" }, fontSize = 22.sp, maxLines = 1)
                Text(
                    target.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(TARGET_LABEL_W),
                )
            }
        }

        // Theme chips — the color→theme schema legend.
        if (chipTags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                chipTags.forEach { tag ->
                    val c = themeColor(tag, dark)
                    Surface(
                        color = c.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 4.dp),
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

@Composable
private fun RelatedEmoji(emoji: String) {
    Text(emoji.ifEmpty { "•" }, fontSize = 15.sp, maxLines = 1)
}

@Composable
private fun RelatedYear(label: String, color: Color) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun RelatedTitle(title: String, color: Color) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(RELATED_LABEL_W),
    )
}
