package com.fluxmusic.player.domain.model

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val currentPosition: Long,
    val duration: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode
) {
    companion object {
        val EMPTY = PlaybackState(null, false, 0L, 0L, false, RepeatMode.OFF)
    }
}