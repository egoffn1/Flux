package com.fluxmusic.player.data.local.dao

import androidx.room.*
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN favorites f ON t.id = f.trackId
        ORDER BY f.addedAt DESC
    """)
    fun getAllFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)

    @Transaction
    suspend fun toggleFavorite(trackId: Long) {
        if (isFavorite(trackId)) {
            removeFavorite(trackId)
        } else {
            addFavorite(FavoriteEntity(trackId = trackId))
        }
    }
}