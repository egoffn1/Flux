package com.fluxmusic.player.playback

import com.fluxmusic.player.domain.model.RepeatMode
import com.fluxmusic.player.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor() {

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var originalQueue: List<Track> = emptyList()

    val currentTrack: Track?
        get() = _queue.value.getOrNull(_currentIndex.value)

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        originalQueue = tracks
        _queue.value = if (_shuffleEnabled.value) tracks.shuffled() else tracks
        _currentIndex.value = if (_shuffleEnabled.value) {
            _queue.value.indexOfFirst { it.id == tracks[startIndex].id }.takeIf { it >= 0 } ?: 0
        } else startIndex
    }

    fun addToQueue(track: Track) {
        _queue.value = _queue.value + track
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
        if (index < _currentIndex.value) {
            _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
        } else if (index == _currentIndex.value && _currentIndex.value >= _queue.value.size) {
            _currentIndex.value = (_queue.value.size - 1).coerceAtLeast(0)
        }
    }

    fun skipToNext(): Track? {
        if (_queue.value.isEmpty()) return null
        _currentIndex.value = when (_repeatMode.value) {
            RepeatMode.OFF -> (_currentIndex.value + 1).coerceAtMost(_queue.value.lastIndex)
            RepeatMode.ALL -> (_currentIndex.value + 1) % _queue.value.size
            RepeatMode.ONE -> _currentIndex.value
        }
        return currentTrack
    }

    fun skipToPrevious(): Track? {
        if (_queue.value.isEmpty()) return null
        _currentIndex.value = when (_repeatMode.value) {
            RepeatMode.OFF -> (_currentIndex.value - 1).coerceAtLeast(0)
            RepeatMode.ALL -> if (_currentIndex.value == 0) _queue.value.lastIndex else _currentIndex.value - 1
            RepeatMode.ONE -> _currentIndex.value
        }
        return currentTrack
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        if (_shuffleEnabled.value) {
            val current = currentTrack
            _queue.value = originalQueue.shuffled()
            if (current != null) {
                val newIndex = _queue.value.indexOfFirst { it.id == current.id }
                if (newIndex >= 0) _currentIndex.value = newIndex
            }
        } else {
            val current = currentTrack
            _queue.value = originalQueue
            if (current != null) {
                val newIndex = _queue.value.indexOfFirst { it.id == current.id }
                if (newIndex >= 0) _currentIndex.value = newIndex
            }
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun seekTo(index: Int) {
        if (index in _queue.value.indices) {
            _currentIndex.value = index
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = 0
        originalQueue = emptyList()
    }
}