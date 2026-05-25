package com.fluxmusic.player.playback

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEqualizer @Inject constructor() {

    companion object {
        private const val TAG = "AudioEqualizer"
        const val PRESET_NORMAL = 0
        const val PRESET_CLASSICAL = 1
        const val PRESET_DANCE = 2
        const val PRESET_FLAT = 3
        const val PRESET_FOLK = 4
        const val PRESET_HEAVY_METAL = 5
        const val PRESET_HIP_HOP = 6
        const val PRESET_JAZZ = 7
        const val PRESET_POP = 8
        const val PRESET_ROCK = 9

        val PRESET_NAMES = mapOf(
            PRESET_NORMAL to "Normal",
            PRESET_CLASSICAL to "Classical",
            PRESET_DANCE to "Dance",
            PRESET_FLAT to "Flat",
            PRESET_FOLK to "Folk",
            PRESET_HEAVY_METAL to "Heavy Metal",
            PRESET_HIP_HOP to "Hip Hop",
            PRESET_JAZZ to "Jazz",
            PRESET_POP to "Pop",
            PRESET_ROCK to "Rock"
        )

        private val PRESET_BANDS = mapOf(
            PRESET_CLASSICAL to shortArrayOf(-400, -300, 0, 300, 400, 500, 400, 300),
            PRESET_DANCE to shortArrayOf(500, 400, 200, 0, -200, -100, 300, 500),
            PRESET_FLAT to shortArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            PRESET_FOLK to shortArrayOf(200, 100, 0, 100, 200, 300, 200, 100),
            PRESET_HEAVY_METAL to shortArrayOf(600, 400, -200, -400, -200, 200, 500, 600),
            PRESET_HIP_HOP to shortArrayOf(500, 300, -200, -300, 0, 200, 300, 400),
            PRESET_JAZZ to shortArrayOf(300, 200, 100, 0, 100, 200, 300, 400),
            PRESET_POP to shortArrayOf(-200, 100, 400, 500, 400, 100, -100, -200),
            PRESET_ROCK to shortArrayOf(500, 400, -200, -400, -200, 300, 500, 600)
        )
    }

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentPreset: Int = PRESET_NORMAL
    private var audioSessionId: Int = -1

    fun attachToSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (sessionId == audioSessionId && equalizer != null) return
        audioSessionId = sessionId
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId)
            val range = equalizer!!.getBandLevelRange()
            Log.d(TAG, "Equalizer range: ${range[0]} to ${range[1]}, bands: ${equalizer!!.numberOfBands}")
            equalizer!!.enabled = true
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(sessionId)
            applyPreset(currentPreset)
            Log.d(TAG, "Equalizer attached to session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach equalizer", e)
        }
    }

    fun applyPreset(preset: Int) {
        currentPreset = preset
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val presetBands = PRESET_BANDS[preset] ?: PRESET_BANDS[PRESET_FLAT]!!
            val range = eq.getBandLevelRange()
            val minLevel = range[0]
            val maxLevel = range[1]

            for (i in 0 until minOf(bands, presetBands.size)) {
                val level = presetBands[i].coerceIn(minLevel, maxLevel)
                eq.setBandLevel(i.toShort(), level)
            }
            eq.enabled = true
            Log.d(TAG, "Applied preset: ${PRESET_NAMES[preset] ?: preset}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply preset", e)
        }
    }

    fun setBassBoost(level: Float) {
        try {
            loudnessEnhancer?.setTargetGain((level * 3000).toInt())
            Log.d(TAG, "Bass boost set to $level")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bass boost", e)
        }
    }

    fun getBandLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            val bands = eq.numberOfBands.toInt()
            (0 until bands).map { i -> eq.getBandLevel(i.toShort()).toInt() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCenterFrequencies(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            val bands = eq.numberOfBands.toInt()
            (0 until bands).map { i -> eq.getCenterFreq(i.toShort()).toInt() / 1000 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        val eq = equalizer ?: return
        try {
            val range = eq.getBandLevelRange()
            val clamped = level.coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(band.toShort(), clamped.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set band level", e)
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        audioSessionId = -1
    }
}
