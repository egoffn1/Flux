package com.fluxmusic.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.playback.BeatState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackItem(
    track: Track,
    isFavorite: Boolean = false,
    onTrackClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "favoriteColor"
    )

    val transition = rememberInfiniteTransition(label = "trackWave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = kotlin.math.PI.toFloat() * 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trackWavePhase"
    )
    val smoothPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = kotlin.math.PI.toFloat() * 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trackSmoothPhase"
    )
    val waveColor = MaterialTheme.colorScheme.primary
    val trackBarCount = 16
    val trackWaveType = LocalWaveType.current
    val playingTrackId = LocalPlayingTrackId.current
    val isTrackPlaying = playingTrackId == track.id
    val beatIntensity by BeatState.beatIntensity.collectAsState()

    val playFraction by animateFloatAsState(
        targetValue = if (isTrackPlaying) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "trackPlayFraction"
    )

    val iconBgColor = MaterialTheme.colorScheme.surface.let {
        Color(it.red * 0.7f, it.green * 0.7f, it.blue * 0.7f, it.alpha)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isTrackPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onTrackClick,
                    onLongClick = onLongClick,
                    onClickLabel = "Play"
                )
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor)
            ) {
                val amplitudes = FloatArray(trackBarCount)
                val centerY = size.height / 2

                for (i in 0 until trackBarCount) {
                    val normalizedI = i.toFloat() / trackBarCount
                    val rawAmp = sin(normalizedI * PI.toFloat() * 4f + wavePhase)
                    val smooth = (sin(smoothPhase * 0.5f) + 1f) / 2f

                    val dancingAmp = ((rawAmp + 1f) / 2f * 0.7f + 0.15f + smooth * 0.15f)
                    val sleepingAmp = 0.08f + normalizedI * 0.03f
                    val beatBoost = 1f + beatIntensity * 0.5f

                    val blendedAmp = dancingAmp * playFraction + sleepingAmp * (1f - playFraction)
                    val amp = (blendedAmp * beatBoost).coerceIn(0.01f, 1f)
                    amplitudes[i] = amp
                }

                when (trackWaveType) {
                    WaveType.BARS -> {
                        val barWidth = size.width / trackBarCount
                        for (i in 0 until trackBarCount) {
                            val amp = amplitudes[i]
                            val barHeight = (amp * size.height * 0.8f).coerceAtLeast(1f)
                            val x = i * barWidth + barWidth * 0.15f
                            val barW = (barWidth * 0.7f).coerceAtLeast(1f)
                            val barAlpha = (0.15f + amp * 0.85f).coerceIn(0f, 1f)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        waveColor.copy(alpha = barAlpha * 0.4f),
                                        waveColor.copy(alpha = barAlpha)
                                    ),
                                    startY = centerY - barHeight / 2f,
                                    endY = centerY + barHeight / 2f
                                ),
                                topLeft = Offset(x, centerY - barHeight / 2f),
                                size = Size(barW, barHeight),
                                cornerRadius = CornerRadius(barW / 2f)
                            )
                        }
                    }
                    WaveType.SINE -> {
                        val step = size.width / trackBarCount
                        val path = Path()
                        for (i in amplitudes.indices) {
                            val x = i * step
                            val y = centerY - amplitudes[i] * size.height * 0.35f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, waveColor, style = Stroke(width = 2.5f))
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(size.width, centerY)
                        fillPath.lineTo(0f, centerY)
                        fillPath.close()
                        drawPath(fillPath, waveColor.copy(alpha = 0.05f))
                    }
                    WaveType.CIRCULAR -> {
                        val cX = size.width / 2f
                        val cY = size.height / 2f
                        val baseR = minOf(size.width, size.height) * 0.3f
                        val path1 = Path()
                        val path2 = Path()
                        for (i in amplitudes.indices) {
                            val angle = (i.toFloat() / amplitudes.size) * 2f * PI.toFloat()
                            val r = baseR + amplitudes[i] * baseR * 0.5f
                            val r2 = baseR + amplitudes[i] * baseR * 0.3f
                            val x = cX + r * cos(angle)
                            val y = cY + r * sin(angle)
                            val x2 = cX + r2 * cos(angle)
                            val y2 = cY + r2 * sin(angle)
                            if (i == 0) {
                                path1.moveTo(x, y)
                                path2.moveTo(x2, y2)
                            } else {
                                path1.lineTo(x, y)
                                path2.lineTo(x2, y2)
                            }
                        }
                        path1.close()
                        path2.close()
                        drawPath(path1, waveColor.copy(alpha = 0.4f), style = Stroke(width = 2.5f))
                        drawPath(path2, waveColor.copy(alpha = 0.15f), style = Stroke(width = 1.5f))
                    }
                    WaveType.GRADIENT_BARS -> {
                        val barWidth = size.width / trackBarCount
                        for (i in 0 until trackBarCount) {
                            val amp = amplitudes[i]
                            val barHeight = (amp * size.height * 0.85f).coerceAtLeast(1f)
                            val x = i * barWidth + barWidth * 0.05f
                            val barW = (barWidth * 0.9f).coerceAtLeast(1f)
                            val t = i.toFloat() / trackBarCount
                            val startAlpha = (0.1f + (1f - t) * 0.3f).coerceIn(0f, 1f)
                            val endAlpha = (0.15f + t * 0.5f).coerceIn(0f, 1f)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        waveColor.copy(alpha = startAlpha * amp),
                                        waveColor.copy(alpha = endAlpha * amp)
                                    ),
                                    startY = centerY - barHeight / 2f,
                                    endY = centerY + barHeight / 2f
                                ),
                                topLeft = Offset(x, centerY - barHeight / 2f),
                                size = Size(barW, barHeight),
                                cornerRadius = CornerRadius(barW * 0.4f, barW * 0.4f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isTrackPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.scale(if (isFavorite) 1.15f else 1f)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = favoriteColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    if (millis < 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
