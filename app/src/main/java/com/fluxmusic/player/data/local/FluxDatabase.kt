package com.fluxmusic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.RoomSQLiteQuery
import com.fluxmusic.player.data.local.dao.FavoriteDao
import com.fluxmusic.player.data.local.dao.PlaylistDao
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.entity.PlaylistEntity
import com.fluxmusic.player.data.local.entity.PlaylistTrackCrossRef
import com.fluxmusic.player.data.local.entity.TrackEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tracks ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        FavoriteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
}