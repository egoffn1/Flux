package com.fluxmusic.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveProgressBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    isPlaying: Boolean,
    waveColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    animationSpeed: Int = 1000,
    currentTimeText: String = "0:00",
    totalTimeText: String = "0:00"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_progress")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(animationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val playFraction by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "playFraction"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        },
                        onDragEnd = { onValueChangeFinished() },
                        onDragCancel = { onValueChangeFinished() }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(newValue)
                            onValueChangeFinished()
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                val barWidth = size.width / barCount
                val barGap = barWidth * 0.1f
                val barDrawWidth = barWidth - barGap * 2
                val progressX = value * size.width
                val centerY = size.height / 2f

                for (i in 0 until barCount) {
                    val normalizedI = i.toFloat() / barCount
                    val rawAmp = sin(normalizedI * PI.toFloat() * 4 + phase)

                    val dancingAmp = ((rawAmp + 1f) / 2f * 0.5f + 0.35f).coerceIn(0.1f, 1f)
                    val sleepingAmp = 0.06f + normalizedI * 0.04f

                    val amp = (dancingAmp * playFraction + sleepingAmp * (1f - playFraction)).coerceIn(0.02f, 1f)
                    val barHeight = (amp * size.height * 0.65f).coerceAtLeast(2f)
                    val barX = i * barWidth + barGap
                    val barCenterX = barX + barDrawWidth / 2f
                    val isPlayed = barCenterX <= progressX

                    val barColor = if (isPlayed) {
                        Brush.verticalGradient(
                            colors = listOf(waveColor.copy(alpha = 0.6f), waveColor.copy(alpha = 1f)),
                            startY = centerY - barHeight / 2f,
                            endY = centerY + barHeight / 2f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(inactiveColor.copy(alpha = 0.2f), inactiveColor.copy(alpha = 0.4f)),
                            startY = centerY - barHeight / 2f,
                            endY = centerY + barHeight / 2f
                        )
                    }

                    drawRoundRect(
                        brush = barColor,
                        topLeft = Offset(barX, centerY - barHeight / 2f),
                        size = Size(barDrawWidth, barHeight),
                        cornerRadius = CornerRadius(barDrawWidth / 2f, barDrawWidth / 2f)
                    )
                }

                val thumbRadius = size.height * 0.3f
                val thumbGlow = Brush.radialGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.15f * playFraction),
                        waveColor.copy(alpha = 0f)
                    ),
                    radius = thumbRadius * 2.5f,
                    center = Offset(progressX, centerY)
                )
                drawCircle(
                    brush = thumbGlow,
                    radius = thumbRadius * 2.5f,
                    center = Offset(progressX, centerY)
                )
                drawCircle(
                    color = waveColor.copy(alpha = 0.6f + 0.4f * playFraction),
                    radius = thumbRadius,
                    center = Offset(progressX, centerY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f + 0.3f * playFraction),
                    radius = thumbRadius * 0.45f,
                    center = Offset(progressX, centerY)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTimeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            Text(
                text = totalTimeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}
