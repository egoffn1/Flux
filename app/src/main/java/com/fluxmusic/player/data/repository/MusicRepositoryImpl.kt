package com.fluxmusic.player.data.repository

import android.net.Uri
import com.fluxmusic.player.data.local.dao.PlayCountDao
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.PlayCountEntity
import com.fluxmusic.player.data.local.mappers.toDomain
import com.fluxmusic.player.data.local.mappers.toEntity
import com.fluxmusic.player.data.scanner.LocalTrackScanner
import com.fluxmusic.player.data.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val mediaScanner: MediaScanner,
    private val localTrackScanner: LocalTrackScanner,
    private val playCountDao: PlayCountDao
) : MusicRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAllAlbums(): Flow<List<Album>> =
        trackDao.getAllTracks().map { entities ->
            entities.groupBy { it.albumId }
                .map { (_, tracks) ->
                    val first = tracks.first()
                    Album(
                        id = first.albumId,
                        name = first.album,
                        artist = first.artist,
                        albumArtUri = first.albumArtUri?.let { android.net.Uri.parse(it) },
                        trackCount = tracks.size,
                        year = null
                    )
                }
                .sortedBy { it.name }
        }

    override fun getAllArtists(): Flow<List<Artist>> =
        trackDao.getAllTracks().map { entities ->
            entities.groupBy { it.artist }
                .map { (artistName, tracks) ->
                    Artist(
                        id = artistName.hashCode().toLong().let { if (it == 0L) -1L else it },
                        name = artistName,
                        trackCount = tracks.size,
                        albumCount = tracks.map { it.albumId }.distinct().size
                    )
                }
                .sortedBy { it.name }
        }

    override fun getTracksByAlbum(albumId: Long): Flow<List<Track>> =
        trackDao.getTracksByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTracksByArtist(artistName: String): Flow<List<Track>> =
        trackDao.getTracksByArtist(artistName).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchTracks(query: String): Flow<List<Track>> =
        trackDao.searchTracks(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTrackById(id: Long): Track? =
        trackDao.getTrackById(id)?.toDomain()

    override suspend fun scanMediaStore() {
        mediaScanner.scan()
    }

    override suspend fun addLocalTrack(uri: Uri): Result<Track> {
        return localTrackScanner.addTrackFromUri(uri).map { it.toDomain() }
    }

    override suspend fun deleteTrack(trackId: Long) {
        trackDao.deleteTrackById(trackId)
    }

    override fun getFrequentlyPlayedTracks(limit: Int): Flow<List<Track>> =
        playCountDao.getMostPlayed(limit).map { counts ->
            counts.mapNotNull { count ->
                runBlocking(Dispatchers.IO) { trackDao.getTrackById(count.trackId) }?.toDomain()
            }
        }

    override suspend fun incrementPlayCount(trackId: Long) {
        val existing = playCountDao.getPlayCount(trackId)
        if (existing != null) {
            playCountDao.increment(trackId)
        } else {
            playCountDao.upsert(PlayCountEntity(trackId = trackId, count = 1, lastPlayedAt = System.currentTimeMillis()))
        }
    }

    override fun getRecentlyAddedTracks(limit: Int): Flow<List<Track>> =
        trackDao.getRecentlyAddedTracks(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun ensureTrackExists(track: Track) {
        val existing = trackDao.getTrackById(track.id)
        if (existing == null) {
            trackDao.insert(track.toEntity())
        }
    }
}
