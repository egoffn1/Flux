package com.fluxmusic.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WaveType(val label: String) {
    BARS("Bars"),
    SINE("Sine Wave"),
    CIRCULAR("Circular"),
    GRADIENT_BARS("Gradient Bars")
}

val LocalWaveType = staticCompositionLocalOf { WaveType.BARS }
val LocalPlayingTrackId = staticCompositionLocalOf<Long?> { null }

@Composable
fun WaveVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    waveType: WaveType = WaveType.BARS,
    color: Color = Color(0xFF6750A4),
    barCount: Int = 32,
    animationSpeed: Int = 1200,
    amplitudeMultiplier: Float = 1f,
    height: Dp = 48.dp
) {
    val phase = remember { mutableFloatStateOf(0f) }
    val smoothPhase = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(animationSpeed) {
        var lastFrameTime = 0L
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                if (lastFrameTime == 0L) lastFrameTime = frameTimeMillis - 16L
                val delta = (frameTimeMillis - lastFrameTime).coerceAtMost(50L)
                lastFrameTime = frameTimeMillis
                val twoPi = 2f * PI.toFloat()
                phase.floatValue = (phase.floatValue + delta * twoPi / animationSpeed) % twoPi
                val smoothSpeed = (animationSpeed * 1.5f).toInt()
                smoothPhase.floatValue = (smoothPhase.floatValue + delta * twoPi / smoothSpeed) % twoPi
            }
        }
    }

    val playFraction by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "playFraction"
    )

    val alpha = 0.06f + playFraction * 0.94f

    val amplitudes = remember(barCount) { FloatArray(barCount) }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val barWidth = size.width / barCount
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val normalizedI = i.toFloat() / barCount
            val rawAmp = sin(normalizedI * PI.toFloat() * 4 + phase.floatValue)
            val smooth = (sin(smoothPhase.floatValue * 0.5f) + 1f) / 2f

            val dancingAmp = ((rawAmp + 1f) / 2f * 0.7f + 0.15f + smooth * 0.15f)
            val sleepingAmp = 0.08f + normalizedI * 0.03f

            val blendedAmp = dancingAmp * playFraction + sleepingAmp * (1f - playFraction)
            val amp = (blendedAmp * amplitudeMultiplier).coerceIn(0.01f, 1f)
            amplitudes[i] = amp
        }

        when (waveType) {
            WaveType.BARS -> drawBars(amplitudes, barWidth, centerY, color, alpha)
            WaveType.SINE -> drawSineWave(amplitudes, centerY, color, alpha)
            WaveType.CIRCULAR -> drawCircular(amplitudes, color, alpha)
            WaveType.GRADIENT_BARS -> drawGradientBars(amplitudes, barCount, centerY, color, alpha)
        }
    }
}

private fun DrawScope.drawBars(amplitudes: FloatArray, barWidth: Float, centerY: Float, color: Color, alpha: Float) {
    for (i in amplitudes.indices) {
        val amp = amplitudes[i]
        val barHeight = (amp * size.height * 0.8f).coerceAtLeast(1f)
        val x = i * barWidth + barWidth * 0.15f
        val barW = (barWidth * 0.7f).coerceAtLeast(1f)
        val barAlpha = ((0.15f + amp * 0.85f) * alpha).coerceIn(0f, 1f)
        val gradient = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = barAlpha * 0.4f), color.copy(alpha = barAlpha)),
            startY = centerY - barHeight / 2f,
            endY = centerY + barHeight / 2f
        )
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(x, centerY - barHeight / 2f),
            size = Size(barW, barHeight),
            cornerRadius = CornerRadius(barW / 2f)
        )
    }
}

private fun DrawScope.drawSineWave(amplitudes: FloatArray, centerY: Float, color: Color, alpha: Float) {
    val step = size.width / amplitudes.size
    val path = Path()
    for (i in amplitudes.indices) {
        val x = i * step
        val y = centerY - amplitudes[i] * size.height * 0.35f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width = 3f))

    val fillPath = Path()
    fillPath.addPath(path)
    fillPath.lineTo(size.width, centerY)
    fillPath.lineTo(0f, centerY)
    fillPath.close()
    drawPath(fillPath, color.copy(alpha = alpha * 0.05f))
}

private fun DrawScope.drawCircular(amplitudes: FloatArray, color: Color, alpha: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val baseRadius = minOf(size.width, size.height) * 0.3f

    val path = Path()
    val smoothPath = Path()
    for (i in amplitudes.indices) {
        val angle = (i.toFloat() / amplitudes.size) * 2f * PI.toFloat()
        val r = baseRadius + amplitudes[i] * baseRadius * 0.5f
        val r2 = baseRadius + amplitudes[i] * baseRadius * 0.3f
        val x = centerX + r * cos(angle)
        val y = centerY + r * sin(angle)
        val x2 = centerX + r2 * cos(angle)
        val y2 = centerY + r2 * sin(angle)
        if (i == 0) {
            path.moveTo(x, y)
            smoothPath.moveTo(x2, y2)
        } else {
            path.lineTo(x, y)
            smoothPath.lineTo(x2, y2)
        }
    }
    path.close()
    smoothPath.close()
    drawPath(path, color.copy(alpha = alpha * 0.4f), style = Stroke(width = 2.5f))
    drawPath(smoothPath, color.copy(alpha = alpha * 0.15f), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawGradientBars(
    amplitudes: FloatArray, barCount: Int, centerY: Float, color: Color, alpha: Float
) {
    val barWidth = size.width / barCount
    for (i in amplitudes.indices) {
        val amp = amplitudes[i]
        val barHeight = (amp * size.height * 0.85f).coerceAtLeast(1f)
        val x = i * barWidth + barWidth * 0.05f
        val barW = (barWidth * 0.9f).coerceAtLeast(1f)
        val t = i.toFloat() / barCount
        val startAlpha = ((0.1f + (1f - t) * 0.3f) * alpha).coerceIn(0f, 1f)
        val endAlpha = ((0.15f + t * 0.5f) * alpha).coerceIn(0f, 1f)
        val gradient = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = startAlpha * amp),
                color.copy(alpha = endAlpha * amp)
            ),
            startY = centerY - barHeight / 2f,
            endY = centerY + barHeight / 2f
        )
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(x, centerY - barHeight / 2f),
            size = Size(barW, barHeight),
            cornerRadius = CornerRadius(barW * 0.4f, barW * 0.4f)
        )
    }
}
