package com.fluxmusic.player.data.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.local.entity.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao
) {
    suspend fun download(url: String, artist: String, title: String): Result<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${artist} - $title.mp3".replace(Regex("[/\\\\:*?\"<>|]"), "_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadToMediaStore(url, fileName, artist, title)
            } else {
                downloadLegacy(url, fileName, artist, title)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadToMediaStore(url: String, fileName: String, artist: String, title: String): Result<TrackEntity> {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Flux")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(Exception("Failed to create MediaStore entry"))

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                try {
                    conn.inputStream.use { input -> input.copyTo(output) }
                } finally {
                    conn.disconnect()
                }
            }

            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)

            val track = TrackEntity(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                album = "Flux Downloads",
                albumId = 0L,
                duration = getDuration(uri) ?: 0L,
                uri = uri.toString(),
                albumArtUri = null,
                dateAdded = System.currentTimeMillis() / 1000,
                isLocal = true
            )
            trackDao.insert(track)
            return Result.success(track)
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            return Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun downloadLegacy(url: String, fileName: String, artist: String, title: String): Result<TrackEntity> {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val fluxDir = File(musicDir, "Flux")
        fluxDir.mkdirs()
        val file = File(fluxDir, fileName)

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        try {
            FileOutputStream(file).use { output ->
                conn.inputStream.use { input -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }

        val track = TrackEntity(
            id = System.currentTimeMillis(),
            title = title,
            artist = artist,
            album = "Flux Downloads",
            albumId = 0L,
            duration = getDuration(Uri.fromFile(file)) ?: 0L,
            uri = Uri.fromFile(file).toString(),
            albumArtUri = null,
            dateAdded = System.currentTimeMillis() / 1000,
            isLocal = true
        )
        trackDao.insert(track)
        return Result.success(track)
    }

    private fun getDuration(uri: Uri): Long? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            retriever?.release()
        }
    }
}
