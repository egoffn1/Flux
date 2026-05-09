package com.fluxmusic.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fluxmusic.player.domain.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var isUpdatingPosition = false
    private val updatePeriod = 250L

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
            mediaItem?.let { 
                // Update duration when track changes
                handler.postDelayed({
                    _duration.value = mediaController?.duration ?: 0L
                }, 100)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _duration.value = mediaController?.duration ?: 0L
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPosition.value = newPosition.positionMs
        }
    }

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { controller ->
                val pos = controller.currentPosition
                val dur = controller.duration
                if (_currentPosition.value != pos) _currentPosition.value = pos
                if (_duration.value != dur && dur > 0) _duration.value = dur
            }
            if (isUpdatingPosition) {
                handler.postDelayed(this, updatePeriod)
            }
        }
    }

    private fun startPositionUpdates() {
        if (!isUpdatingPosition) {
            isUpdatingPosition = true
            handler.post(positionUpdateRunnable)
        }
    }

    private fun stopPositionUpdates() {
        isUpdatingPosition = false
        handler.removeCallbacks(positionUpdateRunnable)
    }

    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            _isConnected.value = true
            _shuffleEnabled.value = mediaController?.shuffleModeEnabled ?: false
            _repeatMode.value = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
            _duration.value = mediaController?.duration ?: 0L
            _currentPosition.value = mediaController?.currentPosition ?: 0L
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        _isConnected.value = false
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()

    fun playTrack(track: Track) {
        mediaController?.let { controller ->
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        mediaController?.let { controller ->
            // Build list of media items
            val mediaItems = tracks.mapNotNull { track ->
                try {
                    MusicServiceHelper.createMediaItem(track)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (mediaItems.isNotEmpty()) {
                // Clear current queue and set new list
                controller.clearMediaItems()
                controller.addMediaItems(mediaItems)
                // Seek to start index
                if (startIndex > 0 && startIndex < mediaItems.size) {
                    controller.seekTo(startIndex, 0)
                }
                controller.prepare()
                controller.play()
                
                // Update duration after setting media items
                handler.postDelayed({
                    _duration.value = controller.duration.coerceAtLeast(0)
                }, 500)
            }
        }
    }

    fun addToQueue(track: Track) {
        mediaController?.let { controller ->
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.addMediaItem(mediaItem)
        }
    }

    fun playNext(track: Track) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.addMediaItem(currentIndex + 1, mediaItem)
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _currentPosition.value = position
    }

    fun skipToNext() {
        mediaController?.let { controller ->
            // Check if there are more items in the queue
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.repeatMode == Player.REPEAT_MODE_ALL && controller.mediaItemCount > 0) {
                // Loop back to start if repeat all is enabled
                controller.seekTo(0, 0)
            }
        }
    }

    fun skipToPrevious() {
        mediaController?.let { controller ->
            // If more than 3 seconds played, restart current track
            if (controller.currentPosition > 3000) {
                controller.seekTo(0)
            } else if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (controller.repeatMode == Player.REPEAT_MODE_ALL && controller.mediaItemCount > 0) {
                // Loop to end if repeat all is enabled and no previous
                controller.seekTo(controller.mediaItemCount - 1, 0)
            } else {
                controller.seekTo(0)
            }
        }
    }

    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        }
    }

    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    
    val currentTrackIndex: Int
        get() = mediaController?.currentMediaItemIndex ?: 0
    
    val hasNext: Boolean
        get() = mediaController?.hasNextMediaItem() == true
        
    val hasPrevious: Boolean
        get() = mediaController?.hasPreviousMediaItem() == true
}