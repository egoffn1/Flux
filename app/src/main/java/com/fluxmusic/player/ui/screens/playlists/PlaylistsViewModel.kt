package com.fluxmusic.player.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxmusic.player.domain.model.Playlist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.domain.repository.PlaylistRepository
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
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
    private val musicRepository: MusicRepository,
    private val queueManager: QueueManager,
    private val mediaSessionConnection: MediaSessionConnection
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _favoriteTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<Long>> = _favoriteTrackIds.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.getFavoriteIds().collect { ids ->
                _favoriteTrackIds.value = ids
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        playlistTracksCache.remove(playlistId)
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            musicRepository.ensureTrackExists(track)
            playlistRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    private val playlistTracksCache = mutableMapOf<Long, StateFlow<List<Track>>>()

    fun getPlaylistTracks(playlistId: Long): StateFlow<List<Track>> {
        return playlistTracksCache.getOrPut(playlistId) {
            playlistRepository.getPlaylistTracks(playlistId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
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

    fun playTrack(track: Track, tracks: List<Track>) {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        mediaSessionConnection.playTracks(tracks, index)
    }
}
