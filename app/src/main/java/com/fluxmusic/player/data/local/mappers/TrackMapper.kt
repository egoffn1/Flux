package com.fluxmusic.player.data.local.mappers

import android.net.Uri
import com.fluxmusic.player.data.local.entity.TrackEntity
import com.fluxmusic.player.domain.model.Album
import com.fluxmusic.player.domain.model.Artist
import com.fluxmusic.player.domain.model.Track

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    duration = duration,
    uri = Uri.parse(uri),
    albumArtUri = albumArtUri?.let { Uri.parse(it) },
    dateAdded = dateAdded
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    duration = duration,
    uri = uri.toString(),
    albumArtUri = albumArtUri?.toString(),
    dateAdded = dateAdded
)

data class AlbumInfo(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val trackCount: Int,
    val year: Int?
)

fun AlbumInfo.toDomain(): Album = Album(
    id = id,
    name = name,
    artist = artist,
    albumArtUri = albumArtUri?.let { Uri.parse(it) },
    trackCount = trackCount,
    year = year
)

data class ArtistInfo(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

fun ArtistInfo.toDomain(): Artist = Artist(
    id = id,
    name = name,
    trackCount = trackCount,
    albumCount = albumCount
)