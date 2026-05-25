package com.fluxmusic.player.domain.usecases

import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository
) {
    suspend operator fun invoke(limit: Int = 30): List<Track> {
        val favoriteTracks = favoritesRepository.getFavoriteTracks().first()
        if (favoriteTracks.isEmpty()) return emptyList()

        val favoriteIds = favoriteTracks.map { it.id }.toSet()
        val favoriteArtistNames = favoriteTracks.map { it.artist }.filter { it.isNotBlank() }.toSet()
        val favoriteAlbumIds = favoriteTracks.map { it.albumId }.filter { it != 0L }.toSet()

        val allTracks = musicRepository.getAllTracks().first()

        val sameArtistTracks = allTracks.filter { it.artist in favoriteArtistNames }
        val sameAlbumTracks = allTracks.filter { it.albumId in favoriteAlbumIds }

        val recommended = (sameArtistTracks + sameAlbumTracks)
            .distinctBy { it.id }
            .filter { it.id !in favoriteIds }
            .shuffled()

        return recommended.take(limit)
    }
}
