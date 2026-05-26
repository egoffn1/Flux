package com.fluxmusic.player.data.local.dao

import androidx.room.*
import com.fluxmusic.player.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE duration > 0 ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE albumId = :albumId AND duration > 0 ORDER BY title ASC")
    fun getTracksByAlbum(albumId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist AND duration > 0 ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%') AND duration > 0 ORDER BY title ASC")
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLocal = 1 AND duration > 0")
    fun getLocalTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE duration > 0 ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedTracks(limit: Int = 20): Flow<List<TrackEntity>>

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("DELETE FROM tracks WHERE isLocal = 0")
    suspend fun deleteMediaStoreTracks()

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album, albumId = :albumId, duration = :duration, uri = :uri, albumArtUri = :albumArtUri, dateAdded = :dateAdded, isLocal = :isLocal WHERE id = :id")
    suspend fun update(id: Long, title: String, artist: String, album: String, albumId: Long, duration: Long, uri: String, albumArtUri: String?, dateAdded: Long, isLocal: Boolean)

    @Query("SELECT id FROM tracks")
    suspend fun getAllTrackIds(): List<Long>

    @Transaction
    suspend fun replaceAll(tracks: List<TrackEntity>) {
        val existingIds = getAllTrackIds().toMutableSet()
        val newIds = tracks.map { it.id }.toSet()
        val idsToDelete = existingIds - newIds
        if (idsToDelete.isNotEmpty()) {
            deleteTracksByIds(idsToDelete.toList())
        }
        for (track in tracks) {
            if (track.id in existingIds) {
                update(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    albumId = track.albumId,
                    duration = track.duration,
                    uri = track.uri,
                    albumArtUri = track.albumArtUri,
                    dateAdded = track.dateAdded,
                    isLocal = track.isLocal
                )
            } else {
                insert(track)
            }
        }
    }

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<Long>)
}