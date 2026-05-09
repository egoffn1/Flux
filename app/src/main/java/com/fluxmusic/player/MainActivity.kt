package com.fluxmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fluxmusic.player.domain.model.RepeatMode
import com.fluxmusic.player.domain.model.Track
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaSessionConnection: MediaSessionConnection

    @Inject
    lateinit var queueManager: QueueManager

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
                        queueManager = queueManager
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
    queueManager: QueueManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isConnected by mediaSessionConnection.isConnected.collectAsState()
    val isPlaying by mediaSessionConnection.isPlaying.collectAsState()
    val currentPosition by mediaSessionConnection.currentPosition.collectAsState()
    val duration by mediaSessionConnection.duration.collectAsState()

    var currentTrack by remember { mutableLongStateOf(-1L) }
    val track = queueManager.currentTrack
    if (track != null && track.id != currentTrack) {
        currentTrack = track.id
    }

    val showBottomBar = currentDestination?.route in Screen.bottomNavItems.map { it.route }
    val showNowPlaying = currentDestination?.route == Screen.NowPlaying.route

    Scaffold(
        bottomBar = {
            if (showBottomBar && !showNowPlaying) {
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
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                            screen.selectedIcon!! else screen.unselectedIcon!!,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                startDestination = Screen.Library.route
            ) {
                composable(Screen.Library.route) {
                    val viewModel: LibraryViewModel = hiltViewModel()
                    LibraryScreen(
                        onTrackClick = { },
                        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }
                composable(Screen.Playlists.route) {
                    PlaylistsScreen()
                }
                composable(Screen.Search.route) {
                    SearchScreen()
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        track = track,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        shuffleEnabled = queueManager.shuffleEnabled.value,
                        repeatMode = queueManager.repeatMode.value,
                        isFavorite = false,
                        onBackClick = { navController.popBackStack() },
                        onPlayPauseClick = {
                            if (isPlaying) mediaSessionConnection.pause() else mediaSessionConnection.play()
                        },
                        onNextClick = {
                            queueManager.skipToNext()
                            mediaSessionConnection.skipToNext()
                        },
                        onPreviousClick = {
                            queueManager.skipToPrevious()
                            mediaSessionConnection.skipToPrevious()
                        },
                        onSeek = { mediaSessionConnection.seekTo(it) },
                        onShuffleClick = { queueManager.toggleShuffle() },
                        onRepeatClick = { queueManager.toggleRepeatMode() },
                        onFavoriteClick = { }
                    )
                }
            }
        }
    }
}