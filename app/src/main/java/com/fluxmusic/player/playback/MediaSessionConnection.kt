package com.fluxmusic.player.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.MusicRepository
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository
) {
    @Volatile
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    private val pendingActions = ConcurrentLinkedQueue<() -> Unit>()

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

    private val _skipTrigger = MutableStateFlow(0L)
    val skipTrigger: StateFlow<Long> = _skipTrigger.asStateFlow()

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
            val controller = mediaController ?: return
            val currentIdx = controller.currentMediaItemIndex
            val queue = queueManager.getQueue()
            if (currentIdx in queue.indices) {
                val track = queue[currentIdx]
                queueManager.setCurrentTrack(track)
                _skipTrigger.value = System.currentTimeMillis()
                Log.d("FluxMusic", "onMediaItemTransition: currentIdx=$currentIdx, track=${track.title}")
            }
            handler.postDelayed({
                mediaController?.let { _duration.value = it.duration.coerceAtLeast(0) }
            }, 100)
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
        context.startService(Intent(context, MusicService::class.java))

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            mediaController = future.get()
            mediaController?.addListener(playerListener)
            _isConnected.value = true
            _shuffleEnabled.value = mediaController?.shuffleModeEnabled ?: false
            _repeatMode.value = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
            _duration.value = mediaController?.duration ?: 0L
            _currentPosition.value = mediaController?.currentPosition ?: 0L

            // Execute pending actions
            Log.d("FluxMusic", "Executing ${pendingActions.size} pending actions")
            while (true) {
                val action = pendingActions.poll() ?: break
                action()
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        stopPositionUpdates()
        pendingActions.clear()
        mediaController?.removeListener(playerListener)
        mediaController = null
        _isConnected.value = false
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()

    fun playTrack(track: Track) {
        val controller = mediaController
        if (controller == null) {
            Log.d("FluxMusic", "playTrack: controller is null, adding to pending")
            pendingActions.add { doPlayTrack(track) }
            return
        }
        doPlayTrack(track)
    }

    private fun doPlayTrack(track: Track) {
        mediaController?.let { controller ->
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.setMediaItem(mediaItem)
            queueManager.setQueue(listOf(track), 0)
            controller.prepare()
            controller.play()
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val controller = mediaController
        if (controller == null) {
            Log.d("FluxMusic", "playTracks: controller is null, adding to pending")
            pendingActions.add { doPlayTracks(tracks, startIndex) }
            return
        }
        doPlayTracks(tracks, startIndex)
    }

    private fun doPlayTracks(tracks: List<Track>, startIndex: Int) {
        val controller = mediaController ?: return
        val mediaItems = tracks.mapNotNull { track ->
            try {
                MusicServiceHelper.createMediaItem(track)
            } catch (e: Exception) {
                null
            }
        }

        if (mediaItems.isNotEmpty()) {
            queueManager.setQueue(tracks, startIndex)
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()

            handler.postDelayed({
                _duration.value = controller.duration.coerceAtLeast(0)
            }, 500)
        }
    }

    fun addToQueue(track: Track) {
        mediaController?.let { controller ->
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.addMediaItem(mediaItem)
            queueManager.addToQueue(track)
        }
    }

    fun playNext(track: Track) {
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val mediaItem = MusicServiceHelper.createMediaItem(track)
            controller.addMediaItem(currentIndex + 1, mediaItem)
            queueManager.addToQueue(track)
        }
    }

    fun addTracksToEnd(tracks: List<Track>) {
        val controller = mediaController
        if (controller == null) {
            Log.d("FluxMusic", "addTracksToEnd: controller is null")
            return
        }
        val mediaItems = tracks.mapNotNull { track ->
            try {
                MusicServiceHelper.createMediaItem(track)
            } catch (e: Exception) { null }
        }
        if (mediaItems.isEmpty()) return
        controller.addMediaItems(mediaItems)
        for (track in tracks) {
            queueManager.addToQueue(track)
        }
        Log.d("FluxMusic", "addTracksToEnd: added ${tracks.size} tracks")
    }

    fun getMediaController(): androidx.media3.common.Player? = mediaController

    fun seekTo(position: Long) {
        try {
            mediaController?.seekTo(position)
            // Don't update _currentPosition here — wait for onPositionDiscontinuity callback
        } catch (e: Exception) {
            Log.e("FluxMusic", "seekTo failed: ${e.message}")
        }
    }

    fun skipToNext() {
        val controller = mediaController
        if (controller == null) {
            Log.d("FluxMusic", "skipToNext: controller is null, adding to pending")
            pendingActions.add {
                val c = mediaController ?: return@add
                doSkipNext(c)
            }
            return
        }
        doSkipNext(controller)
    }

    private fun doSkipNext(controller: MediaController) {
        val totalItems = controller.mediaItemCount
        Log.d("FluxMusic", "doSkipNext: total=$totalItems")

        if (totalItems == 0) return

        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            handler.postDelayed({
                _currentPosition.value = controller.currentPosition
                val dur = controller.duration
                if (dur > 0) _duration.value = dur
            }, 100)
        } else {
            Log.d("FluxMusic", "doSkipNext: no next item")
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        if (controller == null) {
            Log.d("FluxMusic", "skipToPrevious: controller is null, adding to pending")
            pendingActions.add {
                val c = mediaController ?: return@add
                doSkipPrevious(c)
            }
            return
        }
        doSkipPrevious(controller)
    }

    private fun doSkipPrevious(controller: MediaController) {
        if (controller.mediaItemCount == 0) return

        if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            handler.postDelayed({
                _currentPosition.value = controller.currentPosition
                val dur = controller.duration
                if (dur > 0) _duration.value = dur
            }, 100)
        }
    }

    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            queueManager.toggleShuffle()
        }
    }

    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            queueManager.toggleRepeatMode()
        }
    }

    val currentTrackIndex: Int
        get() = mediaController?.currentMediaItemIndex ?: 0

    val hasNext: Boolean
        get() = mediaController?.hasNextMediaItem() == true

    val hasPrevious: Boolean
        get() = mediaController?.hasPreviousMediaItem() == true
}
