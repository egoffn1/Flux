package com.fluxmusic.player.data.repository

import android.net.Uri
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.mappers.toDomain
import com.fluxmusic.player.data.scanner.LocalTrackScanner
import com.fluxmusic.player.data.scanner.MediaScanner
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
    private val localTrackScanner: LocalTrackScanner
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
                .map { (_, tracks) ->
                    val first = tracks.first()
                    Artist(
                        id = first.id,
                        name = first.artist,
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
}