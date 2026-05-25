package com.fluxmusic.player.playback

import android.os.Handler
import android.os.Looper
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimer @Inject constructor() {

    companion object {
        private const val TAG = "SleepTimer"
    }

    private var handler: Handler? = null
    private var remainingSeconds: Int = 0
    private var totalSeconds: Int = 0
    private var isRunning = false
    private var onTimerFinished: (() -> Unit)? = null
    private val tickListeners = mutableListOf<(Int) -> Unit>()

    val isActive: Boolean get() = isRunning

    fun start(minutes: Int, onFinish: () -> Unit) {
        stop()
        totalSeconds = minutes * 60
        remainingSeconds = totalSeconds
        onTimerFinished = onFinish
        isRunning = true
        Log.d(TAG, "Sleep timer started for $minutes minutes")

        handler = Handler(Looper.getMainLooper())
        handler?.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                remainingSeconds--
                if (remainingSeconds <= 0) {
                    Log.d(TAG, "Sleep timer finished")
                    isRunning = false
                    tickListeners.forEach { it(0) }
                    onTimerFinished?.invoke()
                } else {
                    tickListeners.forEach { it(remainingSeconds) }
                    handler?.postDelayed(this, 1000)
                }
            }
        })
    }

    fun stop() {
        isRunning = false
        handler?.removeCallbacksAndMessages(null)
        handler = null
        remainingSeconds = 0
        tickListeners.clear()
    }

    fun addTickListener(listener: (Int) -> Unit) {
        tickListeners.add(listener)
    }

    fun removeTickListener(listener: (Int) -> Unit) {
        tickListeners.remove(listener)
    }

    val remainingFormatted: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val progress: Float
        get() = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
}
