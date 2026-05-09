package com.fluxmusic.player.domain.repository

import com.fluxmusic.player.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavoriteTracks(): Flow<List<Track>>
    suspend fun isFavorite(trackId: Long): Boolean
    suspend fun addToFavorites(trackId: Long)
    suspend fun removeFromFavorites(trackId: Long)
    suspend fun toggleFavorite(trackId: Long)
}