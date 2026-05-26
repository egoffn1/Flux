package com.fluxmusic.player.playback

import android.net.Uri
import com.fluxmusic.player.domain.model.RepeatMode
import com.fluxmusic.player.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private val track1 = Track(1, "Song A", "Artist", "Album", 1, 200000, Uri.parse("http://a"), null, 1000)
    private val track2 = Track(2, "Song B", "Artist", "Album", 1, 180000, Uri.parse("http://b"), null, 1001)
    private val track3 = Track(3, "Song C", "Artist", "Album", 1, 210000, Uri.parse("http://c"), null, 1002)
    private val track4 = Track(4, "Song D", "Artist", "Album", 1, 190000, Uri.parse("http://d"), null, 1003)

    @Before
    fun setup() {
        queueManager = QueueManager()
    }

    @Test
    fun setQueue_initializesCorrectly() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 1)
        assertEquals(3, queueManager.getQueue().size)
        assertEquals(track2, queueManager.currentTrack.value)
        assertEquals(1, queueManager.currentIndex.value)
    }

    @Test
    fun setQueue_emptyList() {
        queueManager.setQueue(emptyList())
        assertEquals(0, queueManager.getQueue().size)
        assertNull(queueManager.currentTrack.value)
    }

    @Test
    fun addToQueue_appendsTrack() {
        queueManager.setQueue(listOf(track1, track2))
        queueManager.addToQueue(track3)
        assertEquals(3, queueManager.getQueue().size)
        assertEquals(track3, queueManager.getQueue().last())
    }

    @Test
    fun addAllToQueue_appendsAllTracks() {
        queueManager.setQueue(listOf(track1))
        queueManager.addAllToQueue(listOf(track2, track3))
        assertEquals(3, queueManager.getQueue().size)
        assertEquals(track2, queueManager.getQueue()[1])
        assertEquals(track3, queueManager.getQueue()[2])
    }

    @Test
    fun skipToNext_withRepeatOff_atEnd_returnsNull() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 1)
        val result = queueManager.skipToNext()
        assertNull(result)
        assertEquals(track2, queueManager.currentTrack.value)
    }

    @Test
    fun skipToNext_withRepeatOff_midQueue() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)
        val result = queueManager.skipToNext()
        assertEquals(track2, result)
        assertEquals(1, queueManager.currentIndex.value)
    }

    @Test
    fun skipToNext_withRepeatAll_wrapsAround() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 1)
        queueManager.toggleRepeatMode() // OFF -> ALL
        val result = queueManager.skipToNext()
        assertEquals(track1, result)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun skipToNext_withRepeatOne_staysOnSameTrack() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)
        queueManager.toggleRepeatMode() // OFF -> ALL
        queueManager.toggleRepeatMode() // ALL -> ONE
        val result = queueManager.skipToNext()
        assertEquals(track1, result)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun skipToPrevious_withRepeatOff_atStart() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)
        val result = queueManager.skipToPrevious()
        assertEquals(track1, result)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun skipToPrevious_withRepeatAll_wrapsAround() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)
        queueManager.toggleRepeatMode() // OFF -> ALL
        val result = queueManager.skipToPrevious()
        assertEquals(track2, result)
        assertEquals(1, queueManager.currentIndex.value)
    }

    @Test
    fun toggleShuffle_preservesCurrentTrack() {
        queueManager.setQueue(listOf(track1, track2, track3, track4), startIndex = 2)
        assertEquals(track3, queueManager.currentTrack.value)
        queueManager.toggleShuffle()
        assertEquals(track3, queueManager.currentTrack.value)
    }

    @Test
    fun toggleShuffle_twice_restoresOriginalOrder() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)
        queueManager.toggleShuffle()
        queueManager.toggleShuffle()
        assertEquals(track1, queueManager.currentTrack.value)
    }

    @Test
    fun removeFromQueue_beforeCurrentIndex() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 2)
        queueManager.removeFromQueue(0)
        assertEquals(2, queueManager.getQueue().size)
        assertEquals(1, queueManager.currentIndex.value)
        assertEquals(track3, queueManager.currentTrack.value)
    }

    @Test
    fun removeFromQueue_atCurrentIndex() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 1)
        queueManager.removeFromQueue(1)
        assertEquals(2, queueManager.getQueue().size)
        assertEquals(track3, queueManager.currentTrack.value)
    }

    @Test
    fun removeFromQueue_afterCurrentIndex() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)
        queueManager.removeFromQueue(2)
        assertEquals(2, queueManager.getQueue().size)
        assertEquals(0, queueManager.currentIndex.value)
        assertEquals(track1, queueManager.currentTrack.value)
    }

    @Test
    fun removeFromQueue_lastItem_clearsQueue() {
        queueManager.setQueue(listOf(track1), startIndex = 0)
        queueManager.removeFromQueue(0)
        assertEquals(0, queueManager.getQueue().size)
        assertNull(queueManager.currentTrack.value)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun seekTo_validIndex() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)
        queueManager.seekTo(2)
        assertEquals(2, queueManager.currentIndex.value)
        assertEquals(track3, queueManager.currentTrack.value)
    }

    @Test
    fun seekTo_invalidIndex_doesNothing() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)
        queueManager.seekTo(10)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun clearQueue_resetsEverything() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 1)
        queueManager.clearQueue()
        assertEquals(0, queueManager.getQueue().size)
        assertNull(queueManager.currentTrack.value)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun toggleRepeatMode_cyclesCorrectly() {
        assertEquals(RepeatMode.OFF, queueManager.repeatMode.value)
        queueManager.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, queueManager.repeatMode.value)
        queueManager.toggleRepeatMode()
        assertEquals(RepeatMode.ONE, queueManager.repeatMode.value)
        queueManager.toggleRepeatMode()
        assertEquals(RepeatMode.OFF, queueManager.repeatMode.value)
    }

    @Test
    fun setCurrentTrack_matchesByIdAndUri() {
        val qTrack = Track(1, "Song A", "Artist", "Album", 1, 200000, Uri.parse("http://a"), null, 1000)
        val incomingTrack = Track(1, "Song A", "Artist", "Album", 1, 200000, Uri.parse("http://a"), null, 1000)
        queueManager.setQueue(listOf(qTrack, track2))
        queueManager.setCurrentTrack(incomingTrack)
        assertEquals(0, queueManager.currentIndex.value)
        assertEquals(incomingTrack, queueManager.currentTrack.value)
    }

    @Test
    fun setCurrentTrack_matchesByIdOnly() {
        val qTrack = Track(1, "Song A", "Artist", "Album", 1, 200000, Uri.parse("http://a"), null, 1000)
        val incomingTrack = Track(1, "Song A (remix)", "Artist", "Album", 1, 200000, Uri.parse("http://b"), null, 1000)
        queueManager.setQueue(listOf(qTrack, track2))
        queueManager.setCurrentTrack(incomingTrack)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun emptyQueue_skipToNext_returnsNull() {
        assertNull(queueManager.skipToNext())
    }

    @Test
    fun emptyQueue_skipToPrevious_returnsNull() {
        assertNull(queueManager.skipToPrevious())
    }

    @Test
    fun removeFromQueue_invalidIndex_doesNothing() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)
        queueManager.removeFromQueue(-1)
        assertEquals(2, queueManager.getQueue().size)
    }
}
