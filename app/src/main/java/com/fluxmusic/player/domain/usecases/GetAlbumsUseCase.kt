package com.fluxmusic.player.domain.usecases

import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Album>> = repository.getAllAlbums()
}