package com.fluxmusic.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.RoomSQLiteQuery
import com.fluxmusic.player.data.local.dao.FavoriteDao
import com.fluxmusic.player.data.local.dao.PlayCountDao
import com.fluxmusic.player.data.local.dao.PlaylistDao
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.entity.PlayCountEntity
import com.fluxmusic.player.data.local.entity.PlaylistEntity
import com.fluxmusic.player.data.local.entity.PlaylistTrackCrossRef
import com.fluxmusic.player.data.local.entity.TrackEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tracks ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `playlist_tracks` (`playlistId` INTEGER NOT NULL, `trackId` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `trackId`), FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON DELETE CASCADE, FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON DELETE CASCADE)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`trackId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`trackId`), FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON DELETE CASCADE)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `playlist_tracks_new` (`playlistId` INTEGER NOT NULL, `trackId` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `trackId`), FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON DELETE CASCADE, FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON DELETE CASCADE)")
        database.execSQL("INSERT INTO playlist_tracks_new SELECT * FROM playlist_tracks")
        database.execSQL("DROP TABLE playlist_tracks")
        database.execSQL("ALTER TABLE playlist_tracks_new RENAME TO playlist_tracks")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackId ON playlist_tracks(trackId)")

        database.execSQL("CREATE TABLE IF NOT EXISTS `favorites_new` (`trackId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`trackId`), FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON DELETE CASCADE)")
        database.execSQL("INSERT INTO favorites_new SELECT * FROM favorites")
        database.execSQL("DROP TABLE favorites")
        database.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_trackId ON favorites(trackId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `play_count` (`trackId` INTEGER PRIMARY KEY NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, `lastPlayedAt` INTEGER NOT NULL DEFAULT 0)")
    }
}

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        FavoriteEntity::class,
        PlayCountEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playCountDao(): PlayCountDao
}