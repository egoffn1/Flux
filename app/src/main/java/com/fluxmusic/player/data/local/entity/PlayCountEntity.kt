package com.fluxmusic.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_count")
data class PlayCountEntity(
    @PrimaryKey
    val trackId: Long,
    val count: Int = 0,
    val lastPlayedAt: Long = 0
)
