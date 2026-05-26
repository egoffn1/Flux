package com.fluxmusic.player.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTrackScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao
) {
    suspend fun addTrackFromUri(uri: Uri): Result<TrackEntity> = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            var title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: getFileName(uri) ?: "Unknown Track"
            
            var artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: ""

            if (MediaScanner.isUnknownArtist(artist)) {
                val parsed = MediaScanner.parseArtistFromTitle(title)
                if (parsed != null) {
                    title = parsed.second
                    artist = parsed.first
                }
            }
            if (artist.isBlank()) artist = "Unknown Artist"
            
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Unknown Album"
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: 0L

            val trackEntity = TrackEntity(
                id = -(System.currentTimeMillis() and 0x7FFFFFFFFFFFFFFF),
                title = title,
                artist = artist,
                album = album,
                albumId = 0L,
                duration = duration,
                uri = uri.toString(),
                albumArtUri = null,
                dateAdded = System.currentTimeMillis() / 1000,
                isLocal = true
            )

            trackDao.insert(trackEntity)
            Result.success(trackEntity)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            retriever?.release()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.substringBeforeLast(".")
    }
}