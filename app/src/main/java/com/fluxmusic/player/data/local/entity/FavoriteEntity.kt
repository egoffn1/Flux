package com.fluxmusic.player.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index("trackId")]
)
data class FavoriteEntity(
    @PrimaryKey
    val trackId: Long,
    val addedAt: Long = System.currentTimeMillis()
)