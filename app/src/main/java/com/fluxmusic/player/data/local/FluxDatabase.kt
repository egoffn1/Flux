package com.fluxmusic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fluxmusic.player.data.local.dao.FavoriteDao
import com.fluxmusic.player.data.local.dao.PlaylistDao
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.entity.PlaylistEntity
import com.fluxmusic.player.data.local.entity.PlaylistTrackCrossRef
import com.fluxmusic.player.data.local.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
}