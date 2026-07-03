package com.miskibin.poznajswiat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
)

private class Particle(
    val x0: Float,        // 0..1 horizontal start
    val delay: Float,     // 0..0.4 of the animation
    val speed: Float,     // fall speed multiplier
    val sway: Float,      // horizontal sway amplitude (px)
    val swayFreq: Float,
    val spin: Float,      // degrees over lifetime
    val size: Float,      // px
    val color: Color,
)

/** One-shot celebratory confetti rain, drawn above the results content. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(70) {
            Particle(
                x0 = Random.nextFloat(),
                delay = Random.nextFloat() * 0.35f,
                speed = 0.75f + Random.nextFloat() * 0.6f,
                sway = 20f + Random.nextFloat() * 50f,
                swayFreq = 4f + Random.nextFloat() * 6f,
                spin = 360f + Random.nextFloat() * 720f,
                size = 12f + Random.nextFloat() * 14f,
                color = CONFETTI_COLORS.random(),
            )
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(1f, tween(durationMillis = 3200, easing = LinearEasing))
    }
    if (time.value >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        val t = time.value
        for (p in particles) {
            val life = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (life <= 0f) continue
            val y = -60f + (size.height + 120f) * life * p.speed
            if (y > size.height + 30f) continue
            val x = p.x0 * size.width + sin(life * p.swayFreq) * p.sway
            val alpha = if (life > 0.8f) (1f - life) / 0.2f else 1f
            rotate(degrees = p.spin * life, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 4f),
                    size = Size(p.size, p.size / 2f),
                )
            }
        }
    }
}
