package com.sukanth.resonance.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun formatsShortAndLongDurations() {
        assertEquals("0:00", formatPlaybackTime(0L))
        assertEquals("5:59", formatPlaybackTime(359_000L))
        assertEquals("1:02:03", formatPlaybackTime(3_723_000L))
    }

    @Test
    fun negativeDurationsAreClamped() {
        assertEquals("0:00", formatPlaybackTime(-1L))
    }
}

