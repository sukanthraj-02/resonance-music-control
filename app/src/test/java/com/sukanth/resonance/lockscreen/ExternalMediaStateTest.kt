package com.sukanth.resonance.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMediaStateTest {
    @Test
    fun missingArtworkUsesDedicatedEmptyIdentity() {
        val state = ExternalMediaState()

        assertNull(state.artwork)
        assertEquals(Long.MIN_VALUE, state.artworkSignature)
    }

    @Test
    fun lockScreenRequiresAccessConnectionAndLiveSession() {
        assertFalse(ExternalMediaState().isLockScreenEligible)
        assertFalse(
            ExternalMediaState(
                notificationAccessGranted = true,
                connected = true,
                canShowOnLockScreen = false,
            ).isLockScreenEligible,
        )
        assertTrue(
            ExternalMediaState(
                notificationAccessGranted = true,
                connected = true,
                canShowOnLockScreen = true,
            ).isLockScreenEligible,
        )
    }
}
