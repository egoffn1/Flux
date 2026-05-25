package com.fluxmusic.player

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxmusic.player.data.local.FluxDatabase
import com.fluxmusic.player.data.local.entity.PlaylistEntity
import com.fluxmusic.player.data.local.entity.FavoriteEntity
import com.fluxmusic.player.data.local.entity.TrackEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: FluxDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            FluxDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetTracks() {
        val dao = database.trackDao()
        val track = TrackEntity(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumId = 1L,
            duration = 200000L,
            uri = "content://test",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            isLocal = true
        )
        dao.insert(track)

        val tracks = dao.getAllTracks()
        assertNotNull(tracks)
    }

    @Test
    fun insertAndGetPlaylists() {
        val dao = database.playlistDao()
        val id = dao.insertPlaylist(
            PlaylistEntity(name = "Test Playlist")
        )
        assertTrue(id > 0)
    }

    @Test
    fun insertAndGetFavorites() {
        val trackDao = database.trackDao()
        val favDao = database.favoriteDao()

        val track = TrackEntity(
            id = 2L,
            title = "Fav Song",
            artist = "Fav Artist",
            album = "Fav Album",
            albumId = 1L,
            duration = 100000L,
            uri = "content://test",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            isLocal = true
        )
        trackDao.insert(track)

        favDao.addFavorite(FavoriteEntity(trackId = 2L))

        assertTrue(favDao.isFavorite(2L))
        assertFalse(favDao.isFavorite(999L))
    }

    @Test
    fun deleteTracksWithCascade() {
        val trackDao = database.trackDao()
        val favDao = database.favoriteDao()

        val track = TrackEntity(
            id = 3L,
            title = "Delete Me",
            artist = "Artist",
            album = "Album",
            albumId = 1L,
            duration = 100000L,
            uri = "content://test",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            isLocal = false
        )
        trackDao.insert(track)
        favDao.addFavorite(FavoriteEntity(trackId = 3L))

        trackDao.deleteMediaStoreTracks()

        assertFalse(favDao.isFavorite(3L))
    }
}
