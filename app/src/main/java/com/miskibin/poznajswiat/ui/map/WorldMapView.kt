package com.miskibin.poznajswiat.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.data.Country
import com.miskibin.poznajswiat.geo.WorldMap
import kotlin.math.max
import kotlin.math.min

class MapColors(
    val water: Color,
    val land: Color,
    val border: Color,
    val marker: Color,
)

@Composable
fun rememberMapColors(): MapColors {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) MapColors(
            water = Color(0xFF0D1B2A),
            land = Color(0xFF46525C),
            border = Color(0xFF0D1B2A),
            marker = Color(0xFF90A4AE),
        ) else MapColors(
            water = Color(0xFFC9DFF2),
            land = Color(0xFFF3EFE5),
            border = Color(0xFFB0BEC5),
            marker = Color(0xFF78909C),
        )
    }
}

private fun ringsToPath(rings: List<FloatArray>, path: Path = Path()): Path {
    for (ring in rings) {
        path.moveTo(ring[0], ring[1])
        var i = 2
        while (i < ring.size) {
            path.lineTo(ring[i], ring[i + 1]); i += 2
        }
        path.close()
    }
    return path
}

/**
 * Interactive world map: pinch to zoom, drag to pan, double-tap to zoom, tap to
 * select a country. [markerCountries] (microstates without polygons) are drawn
 * as fixed-size dots with a generous touch radius — pass only the ones that are
 * meaningful for the given screen, an always-on swarm of dots is just noise.
 * Changing [resetKey] resets the camera (e.g. between quiz questions).
 */
@Composable
fun WorldMapView(
    map: WorldMap,
    modifier: Modifier = Modifier,
    markerCountries: List<Country> = emptyList(),
    interactive: Boolean = true,
    fillFor: (String) -> Color? = { null },
    onTap: ((String?) -> Unit)? = null,
    focusOn: Country? = null,
    resetKey: Any? = null,
) {
    val colors = rememberMapColors()
    val density = LocalDensity.current

    val paths = remember(map) { map.shapes.mapValues { (_, s) -> ringsToPath(s.rings) } }
    val backgroundPath = remember(map) { ringsToPath(map.background) }
    val markers = remember(map, markerCountries) {
        markerCountries.filter { !it.hasPoly }.map { it to map.project(it.lat, it.lng) }
    }

    var zoom by remember(resetKey) { mutableFloatStateOf(1f) }
    // null = camera untouched -> keep the map centered.
    var panState by remember(resetKey) { mutableStateOf<Offset?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    fun baseScale(size: IntSize): Float =
        if (size == IntSize.Zero) 1f
        else min(size.width / map.width, size.height / map.height)

    fun centeredPan(size: IntSize, s: Float): Offset = Offset(
        (size.width - map.width * s) / 2f,
        (size.height - map.height * s) / 2f,
    )

    fun currentPan(size: IntSize): Offset =
        panState ?: centeredPan(size, baseScale(size) * zoom)

    fun clampPan(size: IntSize, p: Offset): Offset {
        val s = baseScale(size) * zoom
        val cw = size.width.toFloat()
        val ch = size.height.toFloat()
        val mw = map.width * s
        val mh = map.height * s
        return Offset(
            if (mw <= cw) (cw - mw) / 2f else p.x.coerceIn(cw - mw, 0f),
            if (mh <= ch) (ch - mh) / 2f else p.y.coerceIn(ch - mh, 0f),
        )
    }

    if (focusOn != null) {
        LaunchedEffect(focusOn, viewSize) {
            if (viewSize == IntSize.Zero) return@LaunchedEffect
            val bounds: Rect = map.shapes[focusOn.cca2]?.bounds
                ?: map.project(focusOn.lat, focusOn.lng).let {
                    Rect(it.x - 30f, it.y - 30f, it.x + 30f, it.y + 30f)
                }
            val base = baseScale(viewSize)
            val fit = min(
                viewSize.width / (bounds.width * 1.9f),
                viewSize.height / (bounds.height * 1.9f),
            )
            zoom = (fit / base).coerceIn(1f, 12f)
            val s = base * zoom
            panState = clampPan(
                viewSize,
                Offset(
                    viewSize.width / 2f - bounds.center.x * s,
                    viewSize.height / 2f - bounds.center.y * s,
                ),
            )
        }
    }

    var mapModifier = modifier.onSizeChanged { viewSize = it }
    if (interactive) {
        mapModifier = mapModifier
            .pointerInput(map) {
                detectTransformGestures { centroid, gesturePan, gestureZoom, _ ->
                    val old = zoom
                    zoom = (zoom * gestureZoom).coerceIn(1f, 14f)
                    val factor = zoom / old
                    val p = currentPan(size)
                    panState = clampPan(size, centroid - (centroid - p) * factor + gesturePan)
                }
            }
            .pointerInput(map, onTap) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val old = zoom
                        zoom = if (zoom > 3.5f) 1f else (zoom * 2.2f).coerceAtMost(14f)
                        val factor = zoom / old
                        val p = currentPan(size)
                        panState = clampPan(size, tap - (tap - p) * factor)
                    },
                    onTap = { tap ->
                        if (onTap == null) return@detectTapGestures
                        val s = baseScale(size) * zoom
                        val p = currentPan(size)
                        val touchR = with(density) { 18.dp.toPx() }
                        val hitMarker = markers
                            .map { (c, mp) -> c to (mp * s + p - tap).getDistance() }
                            .filter { it.second < touchR }
                            .minByOrNull { it.second }
                        if (hitMarker != null) {
                            onTap(hitMarker.first.cca2)
                        } else {
                            onTap(map.hitTest((tap.x - p.x) / s, (tap.y - p.y) / s))
                        }
                    },
                )
            }
    }

    Canvas(mapModifier.fillMaxSize()) {
        val intSize = IntSize(size.width.toInt(), size.height.toInt())
        val s = baseScale(intSize) * zoom
        val p = panState ?: centeredPan(intSize, s)

        drawRect(colors.water)
        withTransform({
            translate(p.x, p.y)
            scale(s, s, pivot = Offset.Zero)
        }) {
            val strokeW = max(0.4f, 1.2f / s)
            drawPath(backgroundPath, colors.land.copy(alpha = 0.55f))
            for ((code, path) in paths) {
                drawPath(path, fillFor(code) ?: colors.land)
                drawPath(path, colors.border, style = Stroke(strokeW))
            }
        }
        val markerR = 3.5.dp.toPx()
        for ((c, mp) in markers) {
            val sp = mp * s + p
            if (sp.x < -markerR || sp.y < -markerR ||
                sp.x > size.width + markerR || sp.y > size.height + markerR
            ) continue
            drawCircle(fillFor(c.cca2) ?: colors.marker, markerR, sp)
            drawCircle(colors.water, markerR, sp, style = Stroke(max(1f, markerR / 4f)))
        }
    }
}
