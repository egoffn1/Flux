package com.fluxmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.playback.MediaSessionConnection
import com.fluxmusic.player.playback.QueueManager
import com.fluxmusic.player.ui.components.MiniPlayer
import com.fluxmusic.player.ui.navigation.Screen
import com.fluxmusic.player.ui.screens.library.LibraryScreen
import com.fluxmusic.player.ui.screens.library.LibraryViewModel
import com.fluxmusic.player.ui.screens.nowplaying.NowPlayingScreen
import com.fluxmusic.player.ui.screens.playlists.PlaylistsScreen
import com.fluxmusic.player.ui.screens.search.SearchScreen
import com.fluxmusic.player.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.media3.common.Player
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaSessionConnection.connect()

        setContent {
            FluxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        mediaSessionConnection = mediaSessionConnection,
                        queueManager = queueManager,
                        favoritesRepository = favoritesRepository
                    )
                }
            }
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
    favoritesRepository: FavoritesRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPlaying by mediaSessionConnection.isPlaying.collectAsState()
    val currentPosition by mediaSessionConnection.currentPosition.collectAsState()
    val duration by mediaSessionConnection.duration.collectAsState()
    val favoriteIds by favoritesRepository.getFavoriteIds().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    var currentTrack by remember { mutableLongStateOf(-1L) }
    val track = queueManager.currentTrack
    if (track != null && track.id != currentTrack) {
        currentTrack = track.id
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
                    MiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onPlayPauseClick = {
                            if (isPlaying) mediaSessionConnection.pause() else mediaSessionConnection.play()
                        },
                        onNextClick = {
                            queueManager.skipToNext()
                            mediaSessionConnection.skipToNext()
                        },
                        onClick = { navController.navigate(Screen.NowPlaying.route) },
                        visible = track != null
                    )
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
                    val viewModel: LibraryViewModel = hiltViewModel()
                    LibraryScreen(
                        onTrackClick = { },
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
                            if (trackId > 0) {
                                scope.launch {
                                    favoritesRepository.toggleFavorite(trackId)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}