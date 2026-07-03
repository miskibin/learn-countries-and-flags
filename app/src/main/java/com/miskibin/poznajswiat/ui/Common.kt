package com.miskibin.poznajswiat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.data.MASTERY_STREAK
import kotlin.math.min

val CorrectGreen = Color(0xFF2E7D32)
val WrongRed = Color(0xFFC62828)

/** High-contrast answer-feedback colors, adapted to the light/dark theme. */
class AnswerColors(
    val correctContainer: Color,
    val onCorrect: Color,
    val wrongContainer: Color,
    val onWrong: Color,
    val correctAccent: Color,
    val wrongAccent: Color,
)

@Composable
fun rememberAnswerColors(): AnswerColors {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) AnswerColors(
            correctContainer = Color(0xFF1B4D1F),
            onCorrect = Color(0xFFC6ECC7),
            wrongContainer = Color(0xFF5C1A17),
            onWrong = Color(0xFFFFD9D6),
            correctAccent = Color(0xFF81C784),
            wrongAccent = Color(0xFFE57373),
        ) else AnswerColors(
            correctContainer = Color(0xFFB5E2B7),
            onCorrect = Color(0xFF07360A),
            wrongContainer = Color(0xFFF3C1BE),
            onWrong = Color(0xFF450C0A),
            correctAccent = CorrectGreen,
            wrongAccent = WrongRed,
        )
    }
}

/**
 * Flag in a uniform 3:2 frame — real flags come in many aspect ratios
 * (Nepal, Switzerland...), so each one is fitted and centered on a neutral
 * backdrop to keep lists and grids visually tidy.
 */
@Composable
fun FlagImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 10.dp,
) {
    if (resId == 0) return
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .aspectRatio(3f / 2f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(1.dp),
        )
    }
}

@Composable
fun ScoreRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "scoreRing",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = ringWidth.toPx()
            val d = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(
                color = track,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = Size(d, d),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = Size(d, d),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** Small dots showing per-mode mastery streak (0..3). */
@Composable
fun MasteryDots(streak: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(MASTERY_STREAK) { i ->
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        CircleShape,
                    )
                    .let {
                        if (i < streak.coerceAtMost(MASTERY_STREAK)) {
                            it.background(CorrectGreen)
                        } else it
                    },
            )
        }
    }
}

@Composable
fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}
