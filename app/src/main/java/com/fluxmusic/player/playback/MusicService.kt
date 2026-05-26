package com.fluxmusic.player.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
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
import com.fluxmusic.player.domain.repository.FavoritesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var audioEqualizer: AudioEqualizer

    @Inject
    lateinit var favoritesRepository: FavoritesRepository

    @Inject
    lateinit var musicRepository: com.fluxmusic.player.domain.repository.MusicRepository

    private val beatDetector = BeatDetector()

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedTrackId: Long? = null
    private var isFavorite = false
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var contentIntent: PendingIntent

    companion object {
        const val CHANNEL_ID = "flux_music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.fluxmusic.player.ACTION_PLAY"
        const val ACTION_PAUSE = "com.fluxmusic.player.ACTION_PAUSE"
        const val ACTION_NEXT = "com.fluxmusic.player.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.fluxmusic.player.ACTION_PREVIOUS"
        const val ACTION_REWIND = "com.fluxmusic.player.ACTION_REWIND"
        const val ACTION_FORWARD = "com.fluxmusic.player.ACTION_FORWARD"
        const val ACTION_LIKE = "com.fluxmusic.player.ACTION_LIKE"
        private const val TAG = "FluxMusic"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(), true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = newPlayer

        mediaSession = MediaSession.Builder(this, newPlayer)
            .setSessionActivity(contentIntent)
            .setCallback(MediaSessionCallback())
            .build()

        newPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotification()
                loadCurrentAlbumArt()
                checkFavoriteStatus()
                val track = queueManager.currentTrack.value
                if (track != null) {
                    serviceScope.launch {
                        musicRepository.incrementPlayCount(track.id)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && newPlayer.audioSessionId > 0) {
                    audioEqualizer.attachToSession(newPlayer.audioSessionId)
                    beatDetector.attach(newPlayer.audioSessionId)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    audioEqualizer.attachToSession(audioSessionId)
                    beatDetector.attach(audioSessionId)
                }
            }
        })
    }

    private fun updateNotification() {
        mediaSession?.let { session ->
            try {
                startForeground(NOTIFICATION_ID, buildNotification(session.player))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification", e)
            }
        }
    }

    private fun buildNotification(player: Player): Notification {
        val currentTrack = queueManager.currentTrack.value

        val playPauseAction = if (player.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        val likeIcon = if (isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTrack?.title ?: "Flux")
            .setContentText(currentTrack?.artist ?: "")
            .setSubText(currentTrack?.album)
            .setLargeIcon(cachedAlbumArt)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    createPendingIntent(ACTION_PREVIOUS)
                )
            )
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    createPendingIntent(ACTION_NEXT)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "-10s",
                    createPendingIntent(ACTION_REWIND)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "+10s",
                    createPendingIntent(ACTION_FORWARD)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    likeIcon,
                    if (isFavorite) "Remove from favorites" else "Add to favorites",
                    createPendingIntent(ACTION_LIKE)
                )
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flux Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadCurrentAlbumArt() {
        val track = queueManager.currentTrack.value ?: return
        if (track.id == cachedTrackId) return
        serviceScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadAlbumArt(track.albumArtUri)
            }
            cachedAlbumArt = bitmap
            cachedTrackId = track.id
            mediaSession?.let { session ->
                try {
                    startForeground(NOTIFICATION_ID, buildNotification(session.player))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update notification with album art", e)
                }
            }
        }
    }

    private fun checkFavoriteStatus() {
        val trackId = queueManager.currentTrack.value?.id ?: return
        serviceScope.launch {
            try {
                isFavorite = favoritesRepository.isFavorite(trackId)
                mediaSession?.let { session ->
                    startForeground(NOTIFICATION_ID, buildNotification(session.player))
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadAlbumArt(uri: Uri?): Bitmap? {
        if (uri == null) return null
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val art = retriever.embeddedPicture
            if (art != null) {
                BitmapFactory.decodeByteArray(art, 0, art.size)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            retriever?.release()
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> player?.play()
            ACTION_PAUSE -> player?.pause()
            ACTION_NEXT -> player?.seekToNextMediaItem()
            ACTION_PREVIOUS -> player?.seekToPreviousMediaItem()
            ACTION_REWIND -> player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
            ACTION_FORWARD -> player?.let { it.seekTo(it.currentPosition + 10_000) }
            ACTION_LIKE -> {
                queueManager.currentTrack.value?.let { track ->
                    serviceScope.launch {
                        try {
                            favoritesRepository.toggleFavorite(track.id)
                            isFavorite = favoritesRepository.isFavorite(track.id)
                            mediaSession?.let { session ->
                                startForeground(NOTIFICATION_ID, buildNotification(session.player))
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        }
        return START_NOT_STICKY
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
        beatDetector.release()
        serviceScope.coroutineContext.cancelChildren()
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
                val uri = item.requestMetadata.mediaUri
                if (uri != null && uri != Uri.EMPTY) {
                    item
                } else {
                    val fallback = queueManager.currentTrack.value?.uri
                    if (fallback != null) {
                        item.buildUpon().setUri(fallback).build()
                    } else item
                }
            }.toMutableList()
            return com.google.common.util.concurrent.Futures.immediateFuture(updatedMediaItems)
        }
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
