package com.fluxmusic.player.domain.usecases

import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArtistsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Artist>> = repository.getAllArtists()
}