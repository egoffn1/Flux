package com.fluxmusic.player.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.domain.usecases.SearchTracksUseCase
import com.fluxmusic.player.playback.MediaSessionConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTracksUseCase: SearchTracksUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val musicRepository: MusicRepository,
    private val mediaSessionConnection: MediaSessionConnection
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Track>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else searchTracksUseCase(query)
        }
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: Track, tracks: List<Track>) {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        mediaSessionConnection.playTracks(tracks, index)
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
}
