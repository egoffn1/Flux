package com.fluxmusic.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientWaveBackground(
    waveType: String,
    color: Color,
    beatIntensity: Float,
    playFraction: Float,
    barCount: Int,
    speed: Int,
    modifier: Modifier = Modifier
) {
    val smoothPlayFraction by animateFloatAsState(
        targetValue = playFraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ambientPlayFraction"
    )
    if (smoothPlayFraction <= 0.01f) return

    val transition = rememberInfiniteTransition(label = "ambientWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(speed / 2, easing = LinearEasing)
        ),
        label = "ambientPhase"
    )
    val breathePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "breathePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val beatPulse = beatIntensity * 0.3f
        val baseAlpha = 0.05f + 0.15f * smoothPlayFraction + beatPulse * 0.1f
        val waveColor = color.copy(alpha = baseAlpha.coerceIn(0f, 0.45f))

        when (waveType) {
            "Sine" -> drawAmbientSine(w, h, phase, waveColor, breathePhase, beatIntensity, smoothPlayFraction)
            "Circular" -> drawAmbientCircular(w, h, phase, waveColor, breathePhase, beatIntensity, smoothPlayFraction)
            "Gradient" -> drawAmbientGradient(w, h, phase, waveColor, breathePhase, beatIntensity, smoothPlayFraction)
            else -> drawAmbientBars(w, h, phase, waveColor, breathePhase, beatIntensity, barCount, smoothPlayFraction)
        }
    }
}

private fun DrawScope.drawAmbientBars(
    w: Float, h: Float, phase: Float, color: Color, breathe: Float, beat: Float, barCount: Int, playFraction: Float
) {
    val barWidth = w / (barCount * 1.5f)
    val gap = barWidth * 0.5f
    val totalWidth = barWidth + gap
    val halfCount = barCount.coerceAtMost(64)
    val breatheOffset = sin(breathe) * h * 0.06f

    for (i in 0 until halfCount) {
        val x = i * totalWidth + gap / 2f
        val normalized = (sin(phase + i * 0.3f) + 1f) / 2f
        val beatBoost = 1f + beat * normalized * 0.8f
        val amp = (6f + normalized * 32f + breatheOffset) * beatBoost * (0.3f + 0.7f * playFraction)
        val barAlpha = (0.3f + normalized * 0.7f) * color.alpha
        drawRect(
            color = color.copy(alpha = barAlpha.coerceIn(0f, color.alpha)),
            topLeft = Offset(x, h / 2f - amp),
            size = Size(barWidth, amp * 2f)
        )
    }
}

private fun DrawScope.drawAmbientSine(
    w: Float, h: Float, phase: Float, color: Color, breathe: Float, beat: Float, playFraction: Float
) {
    val path = Path()
    val baseAmp = (20f + 40f * beat) * (0.2f + 0.8f * playFraction)
    val breatheFactor = 1f + sin(breathe) * 0.4f
    val steps = (w / 3f).toInt().coerceAtLeast(30)
    val stepX = w / steps

    path.moveTo(0f, h / 2f)
    for (i in 1..steps) {
        val x = i * stepX
        val t = i.toFloat() / steps
        val y = h / 2f + sin(phase * 2f + t * PI.toFloat() * 4f) * baseAmp * breatheFactor
            + sin(phase * 3f + t * PI.toFloat() * 7f) * baseAmp * 0.4f
        path.lineTo(x, y.toFloat())
    }

    drawPath(
        path, color = color,
        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val fillPath = Path()
    fillPath.addPath(path)
    fillPath.lineTo(w, h)
    fillPath.lineTo(0f, h)
    fillPath.close()
    drawPath(
        fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = color.alpha * 0.15f), color.copy(alpha = 0f))
        )
    )
}

private fun DrawScope.drawAmbientCircular(
    w: Float, h: Float, phase: Float, color: Color, breathe: Float, beat: Float, playFraction: Float
) {
    val cx = w / 2f
    val cy = h / 2f
    val baseR = minOf(w, h) * 0.38f
    val breatheFactor = 1f + sin(breathe) * 0.15f
    val sizeFactor = 0.1f + 0.9f * playFraction
    val smoothBeat = 1f + beat * 0.12f
    val pulseR = baseR * breatheFactor * smoothBeat * sizeFactor
    val steps = 80

    val alpha = color.alpha
    val baseAlphaVal = (0.06f * playFraction).coerceIn(0.01f, alpha)
    if (baseAlphaVal <= 0.005f) return

    val path = Path()
    var first = true
    for (i in 0..steps) {
        val angle = i.toFloat() / steps * 2f * PI.toFloat()
        val waveOffset = sin(angle * 3f + phase * 1.5f) * (4f + 6f * beat) * sizeFactor
        val r = pulseR + waveOffset
        val cx2 = cx + cos(angle) * r
        val cy2 = cy + sin(angle) * r
        if (first) {
            path.moveTo(cx2, cy2)
            first = false
        } else {
            path.lineTo(cx2, cy2)
        }
    }
    path.close()

    val glowAlpha = (baseAlphaVal * 0.2f).coerceAtMost(0.06f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = glowAlpha),
                color.copy(alpha = 0f)
            ),
            radius = pulseR * 1.3f,
            center = Offset(cx, cy)
        ),
        radius = pulseR * 1.3f,
        center = Offset(cx, cy)
    )

    drawPath(path, color = color.copy(alpha = (baseAlphaVal * 0.35f).coerceAtMost(alpha)), style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color = color.copy(alpha = (baseAlphaVal * 0.15f).coerceAtMost(alpha)), style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color = color.copy(alpha = (baseAlphaVal * 0.08f).coerceAtMost(alpha)), style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawAmbientGradient(
    w: Float, h: Float, phase: Float, color: Color, breathe: Float, beat: Float, playFraction: Float
) {
    val path = Path()
    val baseAmp = (30f + 60f * beat) * (0.2f + 0.8f * playFraction)
    val breatheFactor = 1f + sin(breathe) * 0.3f
    val steps = (w / 5f).toInt().coerceAtLeast(20)

    path.moveTo(0f, h / 2f)
    for (i in 1..steps) {
        val x = i.toFloat() / steps * w
        val t = i.toFloat() / steps
        val y = h / 2f + sin(phase * 1.5f + t * PI.toFloat() * 3f) * baseAmp * breatheFactor
            + sin(phase * 2.5f + t * PI.toFloat() * 5f) * baseAmp * 0.3f
        path.lineTo(x, y.toFloat())
    }
    path.lineTo(w, h)
    path.lineTo(0f, h)
    path.close()

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = (color.alpha * 0.2f).coerceAtMost(0.12f)),
                color.copy(alpha = 0f)
            )
        )
    )
}
