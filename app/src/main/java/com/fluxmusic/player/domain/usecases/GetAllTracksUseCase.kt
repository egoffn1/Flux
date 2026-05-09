package com.fluxmusic.player.domain.usecases

import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTracksUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Track>> = repository.getAllTracks()
}