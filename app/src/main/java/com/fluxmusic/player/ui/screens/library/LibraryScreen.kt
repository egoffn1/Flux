package com.fluxmusic.player.ui.screens.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.network.SearchResult
import com.fluxmusic.player.ui.components.TrackItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onMyWaveClick: (() -> Unit) = {},
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val tracks by viewModel.sortedTracks.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val frequentlyPlayed by viewModel.frequentlyPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) { permissionLauncher.launch(permission) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.addLocalTrack(it) { success, error ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) "Трек добавлен!" else "Ошибка: ${error ?: "Неизвестная ошибка"}"
                    )
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            kotlinx.coroutines.delay(400)
            viewModel.search(searchQuery)
        } else {
            viewModel.clearSearch()
        }
    }

    LaunchedEffect(downloadStatus) {
        downloadStatus?.let {
            if (!isDownloading) {
                snackbarHostState.showSnackbar(it)
                viewModel.clearDownloadStatus()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        topBar = {
            if (showSearch) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search skysound...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.search(searchQuery) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text("Library", style = MaterialTheme.typography.headlineSmall)
                    },
                        actions = {
                            IconButton(onClick = onMyWaveClick) {
                                Icon(Icons.Default.Explore, contentDescription = "My Wave")
                            }
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasPermission && !isLoading && !showSearch,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить трек")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Grant permission to access music", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else if (showSearch) {
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (searchResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                isDownloading = isDownloading,
                                onDownload = { viewModel.downloadTrack(result) },
                                onPreview = { viewModel.playPreview(result) }
                            )
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No results. Tap search icon to search.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (isDownloading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    downloadStatus?.let {
                        Text(it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                if (pullToRefreshState.isRefreshing) {
                    LaunchedEffect(true) {
                        viewModel.refresh()
                        pullToRefreshState.endRefresh()
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("Tracks (${tracks.size})") })
                            Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("Albums (${albums.size})") })
                            Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) }, text = { Text("Artists (${artists.size})") })
                        }

                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (slideInVertically { height: Int -> height } + fadeIn()).togetherWith(slideOutVertically { height: Int -> -height } + fadeOut())
                            },
                            label = "tab_content"
                        ) { tab ->
                            when (tab) {
                                0 -> TracksList(
                                    tracks = tracks,
                                    favoriteTrackIds = favoriteTrackIds,
                                    onTrackClick = { track ->
                                        viewModel.playTrack(track, tracks)
                                        onNavigateToNowPlaying()
                                    },
                                    onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                                    onAddToPlaylist = { track ->
                                        selectedTrackForPlaylist = track
                                        showAddToPlaylistSheet = true
                                    },
                                    recentlyAdded = recentlyAdded,
                                    frequentlyPlayed = frequentlyPlayed
                                )
                                1 -> AlbumsList(
                                    albums = albums,
                                    onAlbumClick = onAlbumClick
                                )
                                2 -> ArtistsList(
                                    artists = artists,
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        if (showAddToPlaylistSheet && selectedTrackForPlaylist != null) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistSheet = false; selectedTrackForPlaylist = null },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)
                ) {
                    Text("Добавить в плейлист", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    Text(selectedTrackForPlaylist?.title ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))

                    if (playlists.isEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text("Нет плейлистов", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Создайте плейлист в разделе Playlists", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedTrackForPlaylist?.let { track ->
                                        viewModel.addTrackToPlaylist(track, playlist.id)
                                        scope.launch {
                                            sheetState.hide()
                                            showAddToPlaylistSheet = false
                                            selectedTrackForPlaylist = null
                                            snackbarHostState.showSnackbar("Добавлено в \"${playlist.name}\"")
                                        }
                                    }
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${playlist.trackCount} треков", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedTrackForPlaylist?.let { track ->
                                viewModel.deleteTrack(track)
                                scope.launch {
                                    sheetState.hide()
                                    showAddToPlaylistSheet = false
                                    selectedTrackForPlaylist = null
                                }
                            }
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Удалить трек", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onPreview: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(result.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            if (result.duration.isNotEmpty()) {
                Text(result.duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onPreview) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = onDownload, enabled = !isDownloading) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TracksList(
    tracks: List<Track>,
    favoriteTrackIds: Set<Long>,
    onTrackClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    recentlyAdded: List<Track> = emptyList(),
    frequentlyPlayed: List<Track> = emptyList()
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState()
    ) {
        if (recentlyAdded.isNotEmpty()) {
            item {
                Text(
                    "Recently Added",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(recentlyAdded.take(5), key = { "recent_${it.id}" }) { track ->
                TrackItem(
                    track = track,
                    isFavorite = track.id in favoriteTrackIds,
                    modifier = Modifier.animateItemPlacement(),
                    onTrackClick = { onTrackClick(track) },
                    onFavoriteClick = { onFavoriteClick(track) },
                    onMoreClick = { onAddToPlaylist(track) },
                )
            }
        }

        if (frequentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    "Frequently Played",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(frequentlyPlayed.take(5), key = { "freq_${it.id}" }) { track ->
                TrackItem(
                    track = track,
                    isFavorite = track.id in favoriteTrackIds,
                    modifier = Modifier.animateItemPlacement(),
                    onTrackClick = { onTrackClick(track) },
                    onFavoriteClick = { onFavoriteClick(track) },
                    onMoreClick = { onAddToPlaylist(track) },
                )
            }
        }

        item {
            Text(
                "All Tracks",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(tracks, key = { it.id }) { track ->
            TrackItem(
                track = track,
                isFavorite = track.id in favoriteTrackIds,
                modifier = Modifier.animateItemPlacement(),
                onTrackClick = { onTrackClick(track) },
                onFavoriteClick = { onFavoriteClick(track) },
                onMoreClick = { onAddToPlaylist(track) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumsList(albums: List<Album>, onAlbumClick: (Album) -> Unit = {}) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(albums, key = { it.id }) { album ->
            AlbumItem(album = album, onClick = { onAlbumClick(album) }, modifier = Modifier.animateItemPlacement())
        }
    }
}

@Composable
private fun AlbumItem(album: Album, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(album.name, style = MaterialTheme.typography.bodyLarge)
            Text("${album.artist} \u2022 ${album.trackCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArtistsList(artists: List<Artist>, onArtistClick: (Artist) -> Unit = {}) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(artists) { artist -> ArtistItem(artist = artist, onClick = { onArtistClick(artist) }) }
    }
}

@Composable
private fun ArtistItem(artist: Artist, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge)
            Text("${artist.trackCount} tracks \u2022 ${artist.albumCount} albums", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
