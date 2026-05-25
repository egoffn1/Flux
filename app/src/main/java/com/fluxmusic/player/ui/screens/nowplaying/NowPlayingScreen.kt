package com.fluxmusic.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxmusic.player.domain.model.RepeatMode
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.ui.components.AmbientWaveBackground
import com.fluxmusic.player.ui.components.WaveProgressBar
import com.fluxmusic.player.ui.components.WaveType
import com.fluxmusic.player.ui.components.WaveVisualizer
import com.fluxmusic.player.ui.components.formatDuration
import com.fluxmusic.player.ui.screens.playlists.PlaylistsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: Track?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlaySimilarClick: (() -> Unit)? = null,
    waveEnabled: Boolean = true,
    waveType: WaveType = WaveType.BARS,
    waveBarCount: Int = 28,
    waveSpeed: Int = 1000,
    isMyWaveActive: Boolean = false,
    beatIntensity: Float = 0f,
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: String = "",
    onSleepTimerClick: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val viewModel: PlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsState()
    var showPlaylistSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val displayProgress = if (isDragging) {
        sliderPosition.coerceIn(0f, 1f)
    } else if (duration > 0) {
        (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isMyWaveActive) 800 else 1200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val albumScale = if (isPlaying) pulseScale else 1f

    val iconPlayFraction by animateFloatAsState(
        targetValue = if (isPlaying || isMyWaveActive) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "iconPlayFraction"
    )

    val iconGlowTransition = rememberInfiniteTransition(label = "iconGlow")
    val iconGlowPhase by iconGlowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isMyWaveActive) 800 else 1200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "iconGlowPhase"
    )

    val favoriteTint by animateColorAsState(
        targetValue = if (isFavorite) primaryColor else onSurfaceVariant,
        animationSpec = tween(300),
        label = "favoriteColor"
    )

    val albumBorderColor by animateColorAsState(
        targetValue = if (isMyWaveActive) primaryColor.copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = tween(500),
        label = "albumBorder"
    )

    val playRotation by animateFloatAsState(
        targetValue = if (isPlaying) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "playRotation"
    )

    var wasPlaying by remember { mutableStateOf(isPlaying) }
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying != wasPlaying) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "playButtonScale",
        finishedListener = { wasPlaying = isPlaying }
    )
    if (isPlaying == wasPlaying) {
        wasPlaying = isPlaying
    }

    var skipPress by remember { mutableStateOf(0L) }
    val skipScale by animateFloatAsState(
        targetValue = if (skipPress > 0L) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 900f),
        label = "skipScale",
        finishedListener = { skipPress = 0L }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sleepTimerActive) {
                        IconButton(onClick = onSleepTimerClick ?: {}) {
                            Icon(Icons.Default.Timer, contentDescription = "Sleep Timer")
                        }
                    } else {
                        IconButton(onClick = onSleepTimerClick ?: {}) {
                            Icon(Icons.Default.Timer, contentDescription = "Sleep Timer",
                                tint = onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Album art with wave visualizer background
            Box(
                modifier = Modifier
                    .scale(albumScale)
                    .aspectRatio(1f)
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(surfaceColor)
                    .then(
                        if (isMyWaveActive) Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f),
                                    primaryColor.copy(alpha = 0.1f),
                                    primaryColor.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Ambient atmospheric waves synced to beat
                AmbientWaveBackground(
                    waveType = when (waveType) {
                        WaveType.BARS -> "Bars"
                        WaveType.SINE -> "Sine"
                        WaveType.CIRCULAR -> "Circular"
                        WaveType.GRADIENT_BARS -> "Gradient"
                    },
                    color = primaryColor,
                    beatIntensity = beatIntensity,
                    playFraction = if (isPlaying || isMyWaveActive) 1f else 0f,
                    barCount = waveBarCount,
                    speed = waveSpeed
                )
                if (waveEnabled) {
                    WaveVisualizer(
                        isPlaying = isPlaying || isMyWaveActive,
                        waveType = waveType,
                        color = if (isMyWaveActive) primaryColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.15f),
                        barCount = waveBarCount,
                        animationSpeed = waveSpeed,
                        amplitudeMultiplier = if (isMyWaveActive) 1.2f else 1f,
                        height = 200.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (track != null) {
                    AnimatedContent(
                        targetState = track.id,
                        transitionSpec = {
                            fadeIn(tween(300)) + slideInVertically { it / 4 } togetherWith
                            fadeOut(tween(200)) + slideOutVertically { -it / 4 }
                        },
                        label = "albumArtTransition"
                    ) { _ ->
                        AsyncImage(
                            model = track.albumArtUri ?: track.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    val iconPulse = 1f + 0.08f * (kotlin.math.sin(iconGlowPhase) + 1f) / 2f * iconPlayFraction
                    val iconGlow = primaryColor.copy(
                        alpha = (0.08f + 0.2f * (kotlin.math.sin(iconGlowPhase + kotlin.math.PI.toFloat()) + 1f) / 2f) * iconPlayFraction
                    )
                    val isActive = isPlaying || isMyWaveActive

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (isActive) iconPulse else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val glowR = minOf(cx, cy) * (0.5f + 0.3f * iconPlayFraction)
                            drawCircle(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(iconGlow, iconGlow.copy(alpha = 0f)),
                                    radius = glowR
                                ),
                                radius = glowR,
                                center = Offset(cx, cy)
                            )
                        }
                        if (waveEnabled && isActive) {
                            WaveVisualizer(
                                isPlaying = true,
                                waveType = waveType,
                                color = primaryColor.copy(alpha = 0.12f),
                                barCount = waveBarCount.coerceIn(8, 32),
                                animationSpeed = waveSpeed,
                                amplitudeMultiplier = 0.4f,
                                height = 160.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = "\u266B",
                            style = MaterialTheme.typography.displayLarge,
                            color = primaryColor.copy(
                                alpha = (0.3f + 0.7f * iconPlayFraction).coerceIn(0.3f, 1f)
                            ),
                            modifier = Modifier.scale(0.7f + 0.3f * iconPlayFraction)
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
                        AnimatedContent(
                            targetState = track?.id ?: -1L,
                            transitionSpec = {
                                fadeIn(tween(250)) + slideInVertically { it / 6 } togetherWith
                                fadeOut(tween(150)) + slideOutVertically { -it / 6 }
                            },
                            label = "trackInfoTransition"
                        ) { _ ->
                            Column {
                                Text(
                                    text = track?.title ?: "No track",
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = onSurfaceColor
                                )
                                Text(
                                    text = track?.artist ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = favoriteTint
                        )
                    }
                    IconButton(onClick = { showPlaylistSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = "Add to playlist",
                            tint = onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wave progress bar (slider + wave animation)
                WaveProgressBar(
                    value = displayProgress,
                    onValueChange = { value ->
                        if (!isDragging) isDragging = true
                        sliderPosition = value
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeek((sliderPosition * duration).toLong())
                    },
                    isPlaying = isPlaying,
                    waveColor = primaryColor,
                    inactiveColor = onSurfaceVariant.copy(alpha = 0.25f),
                    barCount = waveBarCount,
                    animationSpeed = waveSpeed,
                    currentTimeText = formatDuration(
                        if (isDragging) (sliderPosition * duration).toLong()
                        else currentPosition
                    ),
                    totalTimeText = formatDuration(duration),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

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
                            tint = if (shuffleEnabled) primaryColor else onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            skipPress = System.currentTimeMillis()
                            onPreviousClick()
                        },
                        modifier = Modifier.size(48.dp).scale(skipScale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(32.dp),
                            tint = onSurfaceColor
                        )
                    }
                    val (playIcon, playDesc) = if (isPlaying) {
                        Icons.Default.Pause to "Pause"
                    } else {
                        Icons.Default.PlayArrow to "Play"
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isPlaying) 64.dp else 72.dp)
                            .scale(playButtonScale)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                                    radius = 48.dp.value
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = playIcon,
                                contentDescription = playDesc,
                                modifier = Modifier.size(32.dp).rotate(playRotation),
                                tint = onPrimaryColor
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            skipPress = System.currentTimeMillis()
                            onNextClick()
                        },
                        modifier = Modifier.size(48.dp).scale(skipScale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp),
                            tint = onSurfaceColor
                        )
                    }
                    IconButton(onClick = { onPlaySimilarClick?.invoke() }) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Play Similar",
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onRepeatClick) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) primaryColor else onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showPlaylistSheet && track != null) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Add to playlist",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    } else {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToPlaylist(playlist.id, track)
                                        scope.launch {
                                            sheetState.hide()
                                            showPlaylistSheet = false
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
