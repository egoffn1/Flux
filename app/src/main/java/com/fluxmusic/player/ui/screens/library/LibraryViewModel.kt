package com.fluxmusic.player.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxmusic.player.data.download.DownloadHelper
import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Playlist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.domain.repository.PlaylistRepository
import com.fluxmusic.player.domain.usecases.GetAlbumsUseCase
import com.fluxmusic.player.domain.usecases.GetArtistsUseCase
import com.fluxmusic.player.domain.usecases.GetAllTracksUseCase
import com.fluxmusic.player.domain.usecases.GetRecommendationsUseCase
import com.fluxmusic.player.domain.usecases.GetSkySoundRecommendationsUseCase
import com.fluxmusic.player.network.SearchResult
import com.fluxmusic.player.network.SkySoundApi
import com.fluxmusic.player.playback.MediaSessionConnection
import com.fluxmusic.player.playback.QueueManager
import com.fluxmusic.player.playback.WaveRecommendationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAllTracksUseCase: GetAllTracksUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getArtistsUseCase: GetArtistsUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val getSkySoundRecommendationsUseCase: GetSkySoundRecommendationsUseCase,
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistRepository: PlaylistRepository,
    private val queueManager: QueueManager,
    private val mediaSessionConnection: MediaSessionConnection,
    private val skySoundApi: SkySoundApi,
    private val downloadHelper: DownloadHelper,
    private val waveRecommendationManager: WaveRecommendationManager
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = getAlbumsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = getArtistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<Long>> = _favoriteTrackIds.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    val sortedTracks: StateFlow<List<Track>> = kotlinx.coroutines.flow.combine(
        tracks,
        favoriteTrackIds
    ) { trackList, favoriteIds ->
        val favorites = trackList.filter { it.id in favoriteIds }
        val others = trackList.filter { it.id !in favoriteIds }
        favorites + others
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var isFirstLoad = true

    init {
        viewModelScope.launch {
            favoritesRepository.getFavoriteIds().collect { ids ->
                _favoriteTrackIds.value = ids
            }
        }
        loadMusic()
    }

    private fun loadMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            musicRepository.scanMediaStore()
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadMusic()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun playTrack(track: Track, allTracks: List<Track>) {
        val index = allTracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        mediaSessionConnection.playTracks(allTracks, index)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            try {
                musicRepository.ensureTrackExists(track)
                favoritesRepository.toggleFavorite(track.id)
                _favoriteTrackIds.value = if (track.id in _favoriteTrackIds.value) {
                    _favoriteTrackIds.value - track.id
                } else {
                    _favoriteTrackIds.value + track.id
                }
            } catch (_: Exception) { }
        }
    }

    fun playMyWave() {
        waveRecommendationManager.start()
    }

    val isMyWaveActive: kotlinx.coroutines.flow.StateFlow<Boolean>
        get() = waveRecommendationManager.isMyWaveActive

    fun playSimilar(track: Track) {
        viewModelScope.launch {
            val allTracks = tracks.value
            val similar: List<Track> = allTracks
                .filter { it.id != track.id }
                .filter { it.artist == track.artist || it.albumId == track.albumId }
                .shuffled()
                .take(20)
            val queue = listOf(track) + similar
            mediaSessionConnection.playTracks(queue, 0)
        }
    }

    fun playPause() {
        if (mediaSessionConnection.isPlaying.value) {
            mediaSessionConnection.pause()
        } else {
            mediaSessionConnection.play()
        }
    }

    fun skipNext() {
        mediaSessionConnection.skipToNext()
    }

    fun addLocalTrack(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = musicRepository.addLocalTrack(uri)
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { error -> onResult(false, error.message) }
            )
        }
    }

    fun addTrackToPlaylist(track: Track, playlistId: Long) {
        viewModelScope.launch {
            musicRepository.ensureTrackExists(track)
            playlistRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            musicRepository.deleteTrack(track.id)
        }
    }

    private var searchJob: Job? = null
    fun search(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            _searchResults.value = skySoundApi.search(query)
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
    }

    fun playPreview(result: SearchResult) {
        viewModelScope.launch {
            val downloadUrl = skySoundApi.getDownloadUrl(result.downloadPage)
            val url = downloadUrl ?: result.streamUrl
            val track = Track(
                id = System.nanoTime(),
                title = result.title,
                artist = result.artist,
                album = "",
                albumId = 0L,
                duration = 0L,
                uri = android.net.Uri.parse(url),
                albumArtUri = null,
                dateAdded = System.currentTimeMillis() / 1000
            )
            mediaSessionConnection.playTracks(listOf(track), 0)
        }
    }

    fun downloadTrack(result: SearchResult) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadStatus.value = "Getting download link..."
            try {
                val downloadUrl = skySoundApi.getDownloadUrl(result.downloadPage)
                if (downloadUrl == null) {
                    _downloadStatus.value = "Failed to get download link"
                    _isDownloading.value = false
                    return@launch
                }
                _downloadStatus.value = "Downloading ${result.title}..."
                val outcome = downloadHelper.download(downloadUrl, result.artist, result.title)
                outcome.fold(
                    onSuccess = {
                        _downloadStatus.value = "Downloaded: ${result.title}"
                        _isDownloading.value = false
                        refresh()
                    },
                    onFailure = { error ->
                        _downloadStatus.value = "Error: ${error.message}"
                        _isDownloading.value = false
                    }
                )
            } catch (e: Exception) {
                _downloadStatus.value = "Error: ${e.message}"
                _isDownloading.value = false
            }
        }
    }

    fun clearDownloadStatus() {
        _downloadStatus.value = null
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}
