package com.fluxmusic.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluxmusic.player.data.local.entity.PlayCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayCountDao {
    @Query("SELECT * FROM play_count ORDER BY count DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 20): Flow<List<PlayCountEntity>>

    @Query("SELECT * FROM play_count WHERE trackId = :trackId")
    suspend fun getPlayCount(trackId: Long): PlayCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playCount: PlayCountEntity)

    @Query("UPDATE play_count SET count = count + 1, lastPlayedAt = :now WHERE trackId = :trackId")
    suspend fun increment(trackId: Long, now: Long = System.currentTimeMillis())
}
