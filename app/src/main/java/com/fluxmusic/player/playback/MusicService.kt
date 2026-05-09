package com.fluxmusic.player.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.fluxmusic.player.MainActivity
import com.fluxmusic.player.domain.model.Track
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var queueManager: QueueManager

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var currentBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "flux_music_channel"
        const val NOTIFICATION_ID = 1
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flux Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady != true || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        currentBitmap?.recycle()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): com.google.common.util.concurrent.ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { item ->
                item.buildUpon()
                    .setUri(queueManager.currentTrack?.uri ?: item.requestMetadata.mediaUri)
                    .build()
            }.toMutableList()
            return com.google.common.util.concurrent.Futures.immediateFuture(updatedMediaItems)
        }
    }

    fun createMediaItem(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(track.uri)
                    .build()
            )
            .build()
    }
}

object MusicServiceHelper {
    fun createMediaItem(track: Track): MediaItem {
        val uriString = track.uri.toString()
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uriString)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(uriString))
                    .build()
            )
            .build()
    }
}