package com.fluxmusic.player.data.local.mappers

import android.net.Uri
import com.fluxmusic.player.data.local.entity.TrackEntity
import com.fluxmusic.player.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackMapperTest {

    @Test
    fun entityToDomain_mapsAllFields() {
        val entity = TrackEntity(
            id = 42,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumId = 7,
            duration = 200000,
            uri = "content://media/external/audio/media/42",
            albumArtUri = "content://media/external/audio/albumart/7",
            dateAdded = 1000
        )

        val track = entity.toDomain()

        assertEquals(42L, track.id)
        assertEquals("Test Song", track.title)
        assertEquals("Test Artist", track.artist)
        assertEquals("Test Album", track.album)
        assertEquals(7L, track.albumId)
        assertEquals(200000L, track.duration)
        assertEquals(Uri.parse("content://media/external/audio/media/42"), track.uri)
        assertEquals(Uri.parse("content://media/external/audio/albumart/7"), track.albumArtUri)
        assertEquals(1000L, track.dateAdded)
    }

    @Test
    fun entityToDomain_nullAlbumArtUri() {
        val entity = TrackEntity(
            id = 1, title = "No Art", artist = "A", album = "B",
            albumId = 1, duration = 1000,
            uri = "http://u", albumArtUri = null, dateAdded = 0
        )

        val track = entity.toDomain()
        assertNull(track.albumArtUri)
    }

    @Test
    fun entityToDomain_emptyStringUri() {
        val entity = TrackEntity(
            id = 2, title = "Empty URI", artist = "A", album = "B",
            albumId = 1, duration = 1000,
            uri = "", albumArtUri = null, dateAdded = 0
        )

        val track = entity.toDomain()
        assertEquals(Uri.parse(""), track.uri)
    }

    @Test
    fun domainToEntity_mapsAllFields() {
        val track = Track(
            id = 99,
            title = "Domain Song",
            artist = "Domain Artist",
            album = "Domain Album",
            albumId = 3,
            duration = 300000,
            uri = Uri.parse("http://example.com/song.mp3"),
            albumArtUri = Uri.parse("http://example.com/art.jpg"),
            dateAdded = 2000
        )

        val entity = track.toEntity()

        assertEquals(99L, entity.id)
        assertEquals("Domain Song", entity.title)
        assertEquals("Domain Artist", entity.artist)
        assertEquals("Domain Album", entity.album)
        assertEquals(3L, entity.albumId)
        assertEquals(300000L, entity.duration)
        assertEquals("http://example.com/song.mp3", entity.uri)
        assertEquals("http://example.com/art.jpg", entity.albumArtUri)
        assertEquals(2000L, entity.dateAdded)
    }

    @Test
    fun domainToEntity_nullAlbumArtUri() {
        val track = Track(
            id = 1, title = "T", artist = "A", album = "B",
            albumId = 1, duration = 1000,
            uri = Uri.parse("http://u"), albumArtUri = null, dateAdded = 0
        )

        val entity = track.toEntity()
        assertNull(entity.albumArtUri)
    }

    @Test
    fun domainToEntity_roundTrip() {
        val original = Track(
            id = 7, title = "Round", artist = "Trip", album = "Test",
            albumId = 2, duration = 5000,
            uri = Uri.parse("http://t.com/s"),
            albumArtUri = Uri.parse("http://t.com/a"),
            dateAdded = 3000
        )

        val entity = original.toEntity()
        val restored = entity.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.artist, restored.artist)
        assertEquals(original.album, restored.album)
        assertEquals(original.albumId, restored.albumId)
        assertEquals(original.duration, restored.duration)
        assertEquals(original.uri, restored.uri)
        assertEquals(original.albumArtUri, restored.albumArtUri)
        assertEquals(original.dateAdded, restored.dateAdded)
    }

    @Test
    fun albumInfoToDomain_mapsCorrectly() {
        val info = AlbumInfo(
            id = 1, name = "Album", artist = "Artist",
            albumArtUri = "http://art", trackCount = 10, year = 2024
        )

        val album = info.toDomain()
        assertEquals(1L, album.id)
        assertEquals("Album", album.name)
        assertEquals("Artist", album.artist)
        assertEquals(Uri.parse("http://art"), album.albumArtUri)
        assertEquals(10, album.trackCount)
        assertEquals(2024, album.year)
    }

    @Test
    fun artistInfoToDomain_mapsCorrectly() {
        val info = ArtistInfo(
            id = 5, name = "Artist Name",
            trackCount = 20, albumCount = 3
        )

        val artist = info.toDomain()
        assertEquals(5L, artist.id)
        assertEquals("Artist Name", artist.name)
        assertEquals(20, artist.trackCount)
        assertEquals(3, artist.albumCount)
    }
}
