package com.fluxmusic.player.data.repository

import com.fluxmusic.player.data.local.dao.PlaylistDao
import com.fluxmusic.player.data.local.entity.PlaylistEntity
import com.fluxmusic.player.data.local.entity.PlaylistTrackCrossRef
import com.fluxmusic.player.data.local.mappers.toDomain
import com.fluxmusic.player.domain.model.Playlist
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    trackCount = playlistDao.getPlaylistTrackCount(entity.id),
                    createdAt = entity.createdAt
                )
            }
        }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> =
        playlistDao.getPlaylistTracks(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    override suspend fun deletePlaylist(playlistId: Long) =
        playlistDao.deletePlaylistById(playlistId)

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val maxPosition = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPosition + 1
            )
        )
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)

    override suspend fun reorderTracks(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val tracks = playlistDao.getPlaylistTracksSync(playlistId)
        val mutableTracks = tracks.toMutableList()
        if (fromIndex < 0 || fromIndex >= mutableTracks.size) return
        val to = toIndex.coerceIn(0, mutableTracks.size - 1)
        val item = mutableTracks.removeAt(fromIndex)
        mutableTracks.add(to, item)
        mutableTracks.forEachIndexed { index, track ->
            playlistDao.updateTrackPosition(playlistId, track.id, index)
        }
    }
}