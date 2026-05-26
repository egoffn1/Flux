package com.fluxmusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fluxmusic.player.data.local.UserPreferences
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.playback.BeatState
import com.fluxmusic.player.playback.MediaSessionConnection
import com.fluxmusic.player.playback.QueueManager
import com.fluxmusic.player.playback.SleepTimer
import com.fluxmusic.player.playback.WaveRecommendationManager
import com.fluxmusic.player.ui.components.MiniPlayer
import com.fluxmusic.player.ui.components.LocalPlayingTrackId
import com.fluxmusic.player.ui.components.LocalWaveType
import com.fluxmusic.player.ui.components.WaveType
import com.fluxmusic.player.ui.navigation.Screen
import com.fluxmusic.player.ui.screens.library.LibraryScreen
import com.fluxmusic.player.ui.screens.album.AlbumDetailScreen
import com.fluxmusic.player.ui.screens.artist.ArtistDetailScreen
import com.fluxmusic.player.ui.screens.nowplaying.NowPlayingScreen
import com.fluxmusic.player.ui.screens.playlists.PlaylistsScreen
import com.fluxmusic.player.ui.screens.search.SearchScreen
import com.fluxmusic.player.ui.screens.settings.SettingsScreen
import com.fluxmusic.player.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.media3.common.Player
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaSessionConnection: MediaSessionConnection

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var favoritesRepository: FavoritesRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var sleepTimer: SleepTimer

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var waveRecommendationManager: WaveRecommendationManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // permissions granted or denied — proceed anyway
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaSessionConnection.connect()
        requestAudioPermission()

        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)
            val dynamicColor by userPreferences.dynamicColorEnabled.collectAsState(initial = true)
            FluxTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        mediaSessionConnection = mediaSessionConnection,
                        queueManager = queueManager,
                        favoritesRepository = favoritesRepository,
                        userPreferences = userPreferences,
                        sleepTimer = sleepTimer,
                        musicRepository = musicRepository,
                        waveRecommendationManager = waveRecommendationManager
                    )
                }
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(permission))
        }
    }

    override fun onDestroy() {
        mediaSessionConnection.disconnect()
        super.onDestroy()
    }
}

@Composable
fun MainScreen(
    mediaSessionConnection: MediaSessionConnection,
    queueManager: QueueManager,
    favoritesRepository: FavoritesRepository,
    userPreferences: UserPreferences,
    sleepTimer: SleepTimer,
    musicRepository: MusicRepository,
    waveRecommendationManager: WaveRecommendationManager
) {
    val navController = rememberNavController()
    var isFreshStart by rememberSaveable { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPlaying by mediaSessionConnection.isPlaying.collectAsState()
    val currentPosition by mediaSessionConnection.currentPosition.collectAsState()
    val duration by mediaSessionConnection.duration.collectAsState()
    val favoriteIds by favoritesRepository.getFavoriteIds().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val waveEnabled by userPreferences.waveEnabled.collectAsState(initial = false)
    val waveTypePref by userPreferences.waveType.collectAsState(initial = 0)
    val waveBarCount by userPreferences.waveBarCount.collectAsState(initial = 28)
    val waveSpeed by userPreferences.waveSpeed.collectAsState(initial = 1000)
    val sleepTimerActive = sleepTimer.isActive
    val sleepTimerMinutes by userPreferences.sleepTimerMinutes.collectAsState(initial = 15)

    val currentWaveType = when (waveTypePref) {
        0 -> WaveType.BARS
        1 -> WaveType.SINE
        2 -> WaveType.CIRCULAR
        3 -> WaveType.GRADIENT_BARS
        else -> WaveType.BARS
    }

    val track by queueManager.currentTrack.collectAsState()
    val isMyWaveActive by waveRecommendationManager.isMyWaveActive.collectAsState()

    LaunchedEffect(isFreshStart) {
        if (isFreshStart) {
            isFreshStart = false
            navController.navigate(Screen.Library.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentDestination?.route in Screen.bottomNavItems.map { it.route }
    val showNowPlaying = currentDestination?.route == Screen.NowPlaying.route

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && !showNowPlaying,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    AnimatedVisibility(
                        visible = track != null && showBottomBar && !showNowPlaying,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(200)
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        MiniPlayer(
                            track = track,
                            isPlaying = isPlaying,
                            progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onPlayPauseClick = {
                                if (isPlaying) mediaSessionConnection.pause() else mediaSessionConnection.play()
                            },
                            onPreviousClick = {
                                mediaSessionConnection.skipToPrevious()
                            },
                            onNextClick = {
                                mediaSessionConnection.skipToNext()
                            },
                            onClick = { navController.navigate(Screen.NowPlaying.route) },
                            visible = track != null
                        )
                    }
                    NavigationBar {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val playingTrackId = if (isPlaying && track != null) track!!.id else null
            CompositionLocalProvider(
                LocalWaveType provides currentWaveType,
                LocalPlayingTrackId provides playingTrackId
            ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) + fadeOut(tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) + fadeOut(tween(300))
                }
            ) {
                composable(
                    Screen.Library.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    val libraryViewModel: com.fluxmusic.player.ui.screens.library.LibraryViewModel = hiltViewModel()
                    LibraryScreen(
                        onAlbumClick = { album ->
                            navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                        },
                        onArtistClick = { artist ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artist.name))
                        },
                        onMyWaveClick = {
                            libraryViewModel.playMyWave()
                            navController.navigate(Screen.NowPlaying.route)
                        },
                        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }

                composable(
                    Screen.Playlists.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    PlaylistsScreen()
                }

                composable(
                    Screen.Search.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    SearchScreen()
                }

                composable(
                    Screen.Settings.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    SettingsScreen()
                }

                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
                    val libraryViewModel: com.fluxmusic.player.ui.screens.library.LibraryViewModel = hiltViewModel()
                    val albums by libraryViewModel.albums.collectAsState()
                    val tracks by libraryViewModel.sortedTracks.collectAsState()
                    val album = albums.find { it.id == albumId }
                    val albumTracks = tracks.filter { it.albumId == albumId }

                    AlbumDetailScreen(
                        album = album,
                        tracks = albumTracks,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track ->
                            libraryViewModel.playTrack(track, albumTracks)
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }

                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) { backStackEntry ->
                    val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
                    val libraryViewModel: com.fluxmusic.player.ui.screens.library.LibraryViewModel = hiltViewModel()
                    val tracks by libraryViewModel.sortedTracks.collectAsState()
                    val artistTracks = tracks.filter { it.artist == artistName }

                    ArtistDetailScreen(
                        artistName = artistName,
                        tracks = artistTracks,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track ->
                            libraryViewModel.playTrack(track, artistTracks)
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }

                composable(
                    Screen.NowPlaying.route,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(400)
                        ) + fadeIn(tween(400)) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(400)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(400)
                        ) + fadeOut(tween(400)) + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(400)
                        )
                    }
                ) {
                    val shuffleEnabled by mediaSessionConnection.shuffleEnabled.collectAsState()
                    val repeatMode by mediaSessionConnection.repeatMode.collectAsState()
                    val trackId = track?.id ?: -1L
                    val beatIntensity by BeatState.beatIntensity.collectAsState()

                    val waveType = when (waveTypePref) {
                        0 -> WaveType.BARS
                        1 -> WaveType.SINE
                        2 -> WaveType.CIRCULAR
                        3 -> WaveType.GRADIENT_BARS
                        else -> WaveType.BARS
                    }

                    NowPlayingScreen(
                        track = track,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> com.fluxmusic.player.domain.model.RepeatMode.ONE
                            Player.REPEAT_MODE_ALL -> com.fluxmusic.player.domain.model.RepeatMode.ALL
                            else -> com.fluxmusic.player.domain.model.RepeatMode.OFF
                        },
                        isFavorite = trackId in favoriteIds,
                        onBackClick = { navController.popBackStack() },
                        onPlayPauseClick = {
                            if (isPlaying) mediaSessionConnection.pause() else mediaSessionConnection.play()
                        },
                        onNextClick = { mediaSessionConnection.skipToNext() },
                        onPreviousClick = { mediaSessionConnection.skipToPrevious() },
                        onSeek = { mediaSessionConnection.seekTo(it) },
                        onShuffleClick = { mediaSessionConnection.toggleShuffle() },
                        onRepeatClick = { mediaSessionConnection.toggleRepeatMode() },
                        onFavoriteClick = {
                            track?.let { t ->
                                scope.launch {
                                    try {
                                        musicRepository.ensureTrackExists(t)
                                        favoritesRepository.toggleFavorite(t.id)
                                    } catch (_: Exception) { }
                                }
                            }
                        },
                        onPlaySimilarClick = {
                            track?.let { t ->
                                scope.launch {
                                    val allTracks = musicRepository.getAllTracks().first()
                                    val similar = allTracks
                                        .filter { it.id != t.id }
                                        .filter { it.artist == t.artist || it.albumId == t.albumId }
                                        .shuffled()
                                        .take(20)
                                    val queue = listOf(t) + similar
                                    queueManager.setQueue(queue, 0)
                                    mediaSessionConnection.playTracks(queue, 0)
                                }
                            }
                        },
                        waveEnabled = waveEnabled || isMyWaveActive,
                        waveType = waveType,
                        waveBarCount = waveBarCount,
                        waveSpeed = waveSpeed,
                        isMyWaveActive = isMyWaveActive,
                        beatIntensity = beatIntensity,
                        sleepTimerActive = sleepTimerActive,
                        onSleepTimerClick = {
                            if (sleepTimerActive) {
                                sleepTimer.stop()
                            } else {
                                val minutes = sleepTimerMinutes.coerceIn(1, 120)
                                sleepTimer.start(minutes) {
                                    mediaSessionConnection.pause()
                                }
                            }
                        }
                    )
                }
            }
            }
        }
    }
}
