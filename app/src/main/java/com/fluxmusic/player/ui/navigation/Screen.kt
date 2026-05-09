package com.fluxmusic.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    data object Tracks : Screen(route = "tracks", title = "Tracks")

    data object Albums : Screen(route = "albums", title = "Albums")

    data object Artists : Screen(route = "artists", title = "Artists")

    data object Playlists : Screen(
        route = "playlists",
        title = "Playlists",
        selectedIcon = Icons.Filled.PlaylistPlay,
        unselectedIcon = Icons.Outlined.PlaylistPlay
    )

    data object Search : Screen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    data object NowPlaying : Screen(route = "now_playing", title = "Now Playing")

    data object AlbumDetail : Screen(route = "album/{albumId}", title = "Album") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }

    data object ArtistDetail : Screen(route = "artist/{artistName}", title = "Artist") {
        fun createRoute(artistName: String) = "artist/$artistName"
    }

    data object PlaylistDetail : Screen(route = "playlist/{playlistId}", title = "Playlist") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    companion object {
        val bottomNavItems = listOf(Library, Playlists, Search)
    }
}