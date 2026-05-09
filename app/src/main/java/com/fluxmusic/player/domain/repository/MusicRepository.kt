package com.fluxmusic.player.domain.repository

import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllTracks(): Flow<List<Track>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getAllArtists(): Flow<List<Artist>>
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>
    fun getTracksByArtist(artistName: String): Flow<List<Track>>
    fun searchTracks(query: String): Flow<List<Track>>
    suspend fun getTrackById(id: Long): Track?
    suspend fun scanMediaStore()
    suspend fun addLocalTrack(uri: android.net.Uri): Result<Track>
}