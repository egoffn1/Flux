package com.fluxmusic.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {

    @Test
    fun formatDuration_zero() {
        assertEquals("0:00", formatDuration(0))
    }

    @Test
    fun formatDuration_seconds() {
        assertEquals("0:05", formatDuration(5000))
        assertEquals("0:30", formatDuration(30000))
        assertEquals("0:59", formatDuration(59000))
    }

    @Test
    fun formatDuration_minutes() {
        assertEquals("1:00", formatDuration(60000))
        assertEquals("3:30", formatDuration(210000))
        assertEquals("59:59", formatDuration(3599000))
    }

    @Test
    fun formatDuration_hours() {
        assertEquals("60:00", formatDuration(3600000))
        assertEquals("90:05", formatDuration(5405000))
    }

    @Test
    fun formatDuration_rounding() {
        assertEquals("1:00", formatDuration(60999))
        assertEquals("0:00", formatDuration(999))
    }

    @Test
    fun formatDuration_negative() {
        assertEquals("0:00", formatDuration(-1000))
    }
}
