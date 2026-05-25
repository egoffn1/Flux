package com.fluxmusic.player.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BeatState {
    private val _beatIntensity = MutableStateFlow(0f)
    val beatIntensity: StateFlow<Float> = _beatIntensity.asStateFlow()

    private val _bpm = MutableStateFlow(0)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    fun updateBeat(intensity: Float) {
        _beatIntensity.value = intensity
    }

    fun updateBpm(bpm: Int) {
        _bpm.value = bpm
    }

    fun reset() {
        _beatIntensity.value = 0f
        _bpm.value = 0
    }
}
