package com.fluxmusic.player.playback

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sqrt

class BeatDetector {
    companion object {
        private const val TAG = "FluxBeat"
        private const val CAPTURE_SIZE = 256
        private const val ENERGY_HISTORY = 43
        private const val BEAT_THRESHOLD = 1.8f
        private const val SYNTHETIC_BPM = 120
    }

    private var visualizer: Visualizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val synthRunnable = object : Runnable {
        override fun run() {
            if (!useSynthetic) return
            val t = System.currentTimeMillis()
            val beatPhase = ((t % beatInterval) / beatInterval.toFloat()) * 2f * kotlin.math.PI.toFloat()
            val intensity = (kotlin.math.sin(beatPhase) + 1f) / 2f
            val shaped = if (intensity > 0.85f) (intensity - 0.85f) * 6.67f else 0f
            BeatState.updateBeat(shaped.coerceIn(0f, 1f) * 0.6f)
            handler.postDelayed(this, 20)
        }
    }
    private var beatInterval = 60000L / SYNTHETIC_BPM / 2
    private var useSynthetic = true

    private var energyHistory = FloatArray(ENERGY_HISTORY)
    private var historyIndex = 0
    private var lastBeatTime = 0L
    private val beatIntervals = mutableListOf<Long>()

    fun attach(sessionId: Int) {
        release()
        useSynthetic = true
        try {
            val v = Visualizer(sessionId)
            v.captureSize = CAPTURE_SIZE
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(it) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = true
            visualizer = v
            useSynthetic = false
            Log.d(TAG, "Attached real Visualizer to session $sessionId")
        } catch (e: Exception) {
            Log.d(TAG, "Visualizer failed, using synthetic: ${e.message}")
        }
        if (useSynthetic) {
            handler.post(synthRunnable)
        }
    }

    fun release() {
        handler.removeCallbacks(synthRunnable)
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) { }
        visualizer = null
        useSynthetic = true
        BeatState.reset()
    }

    private fun processWaveform(waveform: ByteArray) {
        var sumSquares = 0f
        for (i in waveform.indices) {
            val sample = waveform[i].toFloat() / 128f
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / waveform.size)

        energyHistory[historyIndex % ENERGY_HISTORY] = rms
        historyIndex++

        if (historyIndex >= ENERGY_HISTORY) {
            var sum = 0f
            for (i in 0 until ENERGY_HISTORY) sum += energyHistory[i]
            val avgEnergy = sum / ENERGY_HISTORY
            val instantEnergy = rms

            val now = System.currentTimeMillis()
            if (instantEnergy > avgEnergy * BEAT_THRESHOLD && instantEnergy > 0.15f) {
                val sinceLast = now - lastBeatTime
                if (sinceLast > 200) {
                    lastBeatTime = now
                    beatIntervals.add(sinceLast)
                    if (beatIntervals.size > 10) beatIntervals.removeFirst()

                    if (beatIntervals.size >= 4) {
                        val avgMs = beatIntervals.average()
                        if (avgMs >= 250 && avgMs <= 2000) {
                            BeatState.updateBpm((60000f / avgMs).toInt().coerceIn(40, 200))
                        }
                    }

                    val intensity = ((instantEnergy / avgEnergy - 1.2f) * 2f).coerceIn(0f, 1f)
                    BeatState.updateBeat(intensity)

                    handler.postDelayed({
                        if (BeatState.beatIntensity.value > 0.1f) {
                            BeatState.updateBeat(BeatState.beatIntensity.value * 0.6f)
                        } else {
                            BeatState.reset()
                        }
                    }, 150)
                }
            }
        }
    }
}
