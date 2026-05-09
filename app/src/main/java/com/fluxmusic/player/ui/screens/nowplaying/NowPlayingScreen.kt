package com.fluxmusic.player.ui.screens.nowplaying

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.fluxmusic.player.domain.model.RepeatMode as DomainRepeatMode
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.ui.components.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: Track?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: DomainRepeatMode,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Track ID-based color - only changes when track changes
    var dominantColor by remember(track?.id) { mutableStateOf(ComposeColor(0xFF1DB954)) }
    var vibrantColor by remember(track?.id) { mutableStateOf(ComposeColor(0xFF1ED760)) }
    
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val displayProgress = if (isDragging) {
        sliderPosition.coerceIn(0f, 1f)
    } else if (duration > 0) {
        (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    // Extract colors from album art using Palette
    LaunchedEffect(track?.id, track?.albumArtUri) {
        track?.albumArtUri?.let { uri ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(uri)
                            .allowHardware(false)
                            .build()
                        val result = (loader.execute(request) as? SuccessResult)?.drawable
                        val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap

                        bitmap?.let { bmp ->
                            val colors = extractColors(bmp)
                            dominantColor = colors.first
                            vibrantColor = colors.second
                        }
                    } catch (e: Exception) {
                        // Keep default green colors on error
                    }
                }
            }
        }
    }

    // Wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", color = ComposeColor.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = ComposeColor.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor.Transparent
                )
            )
        },
        containerColor = ComposeColor.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColor,
                            vibrantColor.copy(alpha = 0.7f),
                            ComposeColor(0xFF121212),
                            ComposeColor(0xFF121212)
                        )
                    )
                )
        ) {
            // Blurred album art - beautiful background
            if (track?.albumArtUri != null) {
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(80.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay on top of blurred image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    dominantColor.copy(alpha = 0.4f),
                                    ComposeColor.Black.copy(alpha = 0.7f),
                                    ComposeColor(0xFF121212)
                                )
                            )
                        )
                )
            }

            // Wave animation at bottom when playing
            if (isPlaying) {
                WaveAnimation(
                    waveOffset = waveOffset,
                    color = vibrantColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Album art with glow effect
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(vibrantColor.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track?.albumArtUri != null) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = ComposeColor.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Track info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track?.title ?: "No track",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                color = ComposeColor.White
                            )
                            Text(
                                text = track?.artist ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ComposeColor.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                        }
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) ComposeColor(0xFFE91E63) else ComposeColor.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = displayProgress,
                            onValueChange = { value ->
                                if (!isDragging) isDragging = true
                                sliderPosition = value
                                // Update position immediately for smooth UI
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                onSeek((sliderPosition * duration).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = vibrantColor,
                                activeTrackColor = vibrantColor,
                                inactiveTrackColor = ComposeColor.White.copy(alpha = 0.2f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(
                                    if (isDragging) (sliderPosition * duration).toLong()
                                    else currentPosition
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ComposeColor.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = ComposeColor.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onShuffleClick) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleEnabled) vibrantColor else ComposeColor.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onPreviousClick) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp),
                                tint = ComposeColor.White
                            )
                        }
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(vibrantColor)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp),
                                tint = ComposeColor.Black
                            )
                        }
                        IconButton(onClick = onNextClick) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp),
                                tint = ComposeColor.White
                            )
                        }
                        IconButton(onClick = onRepeatClick) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    DomainRepeatMode.ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != DomainRepeatMode.OFF) vibrantColor else ComposeColor.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveAnimation(
    waveOffset: Float,
    color: ComposeColor,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val waveHeight = 60.dp.toPx()
        val path = Path()
        path.moveTo(0f, size.height)

        for (i in 0..size.width.toInt() step 2) {
            val y = size.height - waveHeight -
                (kotlin.math.sin((i + waveOffset) * 0.012f) * 30).toFloat() -
                (kotlin.math.sin((i + waveOffset * 0.5f) * 0.018f) * 20).toFloat() -
                (kotlin.math.sin((i + waveOffset * 0.8f) * 0.025f) * 10).toFloat()
            path.lineTo(i.toFloat(), y)
        }

        path.lineTo(size.width, size.height)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.6f),
                    color.copy(alpha = 0.3f),
                    ComposeColor.Transparent
                )
            )
        )
    }
}

private fun extractColors(bitmap: Bitmap): Pair<ComposeColor, ComposeColor> {
    // Simple color extraction - get dominant and vibrant colors
    val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
    
    var r = 0
    var g = 0
    var b = 0
    var count = 0
    
    val pixels = IntArray(50 * 50)
    scaled.getPixels(pixels, 0, 50, 0, 0, 50, 50)
    
    for (pixel in pixels) {
        r += Color.red(pixel)
        g += Color.green(pixel)
        b += Color.blue(pixel)
        count++
    }
    
    r /= count
    g /= count
    b /= count
    
    // Create dominant color
    val dominant = ComposeColor(
        red = r / 255f,
        green = g / 255f,
        blue = b / 255f,
        alpha = 1f
    )
    
    // Create vibrant version - brighter and more saturated
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val s = if (max > 0) delta / max else 0f
    val l = (max + min) / 2f / 255f
    
    val vibrantR = (r + 30).coerceAtMost(255)
    val vibrantG = (g + 30).coerceAtMost(255)
    val vibrantB = (b + 30).coerceAtMost(255)
    
    val vibrant = ComposeColor(
        red = vibrantR / 255f,
        green = vibrantG / 255f,
        blue = vibrantB / 255f,
        alpha = 1f
    )
    
    scaled.recycle()
    
    return dominant to vibrant
}