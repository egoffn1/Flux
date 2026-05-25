package com.fluxmusic.player.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    private val contentResolver: ContentResolver,
    private val trackDao: TrackDao
) {
    suspend fun scan() = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackEntity>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    cursor.getLong(albumIdColumn)
                )

                var rawArtist = cursor.getString(artistColumn) ?: ""
                var rawTitle = cursor.getString(titleColumn) ?: "Unknown"

                if (isUnknownArtist(rawArtist)) {
                    val parsed = parseArtistFromTitle(rawTitle)
                    if (parsed != null) {
                        rawTitle = parsed.second
                        rawArtist = parsed.first
                    }
                }

                tracks.add(
                    TrackEntity(
                        id = id,
                        title = rawTitle,
                        artist = rawArtist.ifBlank { "Unknown" },
                        album = cursor.getString(albumColumn) ?: "Unknown Album",
                        albumId = cursor.getLong(albumIdColumn),
                        duration = cursor.getLong(durationColumn),
                        uri = contentUri.toString(),
                        albumArtUri = albumArtUri.toString(),
                        dateAdded = cursor.getLong(dateAddedColumn),
                        isLocal = false
                    )
                )
            }
        }

        trackDao.replaceAll(tracks)
    }

    companion object {
        private val SEPARATORS = listOf(" — ", " – ", " - ", " : ", " :: ", " / ", " \\ ", " | ")

        private val KNOWN_UNKNOWN = setOf(
            "<unknown>", "unknown", "unknown artist", "", " ",
            "artist unknown"
        )

        fun isUnknownArtist(artist: String): Boolean =
            artist.isBlank() || artist.trim().lowercase() in KNOWN_UNKNOWN

        fun parseArtistFromTitle(title: String): Pair<String, String>? {
            val trimmed = title.trim()
            for (sep in SEPARATORS) {
                val idx = trimmed.indexOf(sep)
                if (idx > 0) {
                    val possibleArtist = trimmed.substring(0, idx).trim()
                    val possibleTitle = trimmed.substring(idx + sep.length).trim()
                    if (possibleArtist.isNotBlank() && possibleTitle.isNotBlank()
                        && possibleArtist.length < possibleTitle.length * 2
                        && possibleArtist.length in 2..60
                        && !possibleArtist.contains(" - ")
                        && !possibleArtist.all { it.isUpperCase() || it.isDigit() || it == ' ' }
                    ) {
                        return possibleArtist to possibleTitle
                    }
                }
            }
            return null
        }
    }
}