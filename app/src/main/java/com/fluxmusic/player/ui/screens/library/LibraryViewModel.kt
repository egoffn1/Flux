package com.fluxmusic.player.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.domain.usecases.GetAlbumsUseCase
import com.fluxmusic.player.domain.usecases.GetArtistsUseCase
import com.fluxmusic.player.domain.usecases.GetAllTracksUseCase
import com.fluxmusic.player.playback.MediaSessionConnection
import com.fluxmusic.player.playback.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAllTracksUseCase: GetAllTracksUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getArtistsUseCase: GetArtistsUseCase,
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val queueManager: QueueManager,
    private val mediaSessionConnection: MediaSessionConnection
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = getAlbumsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = getArtistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<Long>> = _favoriteTrackIds.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.getFavoriteTracks().collect { tracks ->
                _favoriteTrackIds.value = tracks.map { it.id }.toSet()
            }
        }
    }

    init {
        loadMusic()
    }

    private fun loadMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            musicRepository.scanMediaStore()
            _isLoading.value = false
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun playTrack(track: Track, tracks: List<Track>) {
        val index = tracks.indexOfFirst { it.id == track.id }
        queueManager.setQueue(tracks, index.coerceAtLeast(0))
        mediaSessionConnection.playTracks(tracks, index.coerceAtLeast(0))
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(trackId)
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
        queueManager.skipToNext()
        mediaSessionConnection.skipToNext()
    }

    fun addLocalTrack(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = musicRepository.addLocalTrack(uri)
            result.fold(
                onSuccess = {
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.message)
                }
            )
        }
    }
}