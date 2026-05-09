package com.fluxmusic.player.data.repository

import com.fluxmusic.player.data.local.dao.FavoriteDao
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.mappers.toDomain
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun getFavoriteTracks(): Flow<List<Track>> =
        favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getFavoriteIds(): Flow<Set<Long>> =
        favoriteDao.getFavoriteIds().map { it.toSet() }

    override suspend fun isFavorite(trackId: Long): Boolean =
        favoriteDao.isFavorite(trackId)

    override suspend fun addToFavorites(trackId: Long) =
        favoriteDao.addFavorite(FavoriteEntity(trackId = trackId))

    override suspend fun removeFromFavorites(trackId: Long) =
        favoriteDao.removeFavorite(trackId)

    override suspend fun toggleFavorite(trackId: Long) =
        favoriteDao.toggleFavorite(trackId)
}