package com.fluxmusic.player.domain.usecases

import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTracksUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(query: String): Flow<List<Track>> = repository.searchTracks(query)
}