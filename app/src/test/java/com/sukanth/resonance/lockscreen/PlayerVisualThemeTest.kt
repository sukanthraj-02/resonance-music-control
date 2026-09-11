package com.sukanth.resonance.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVisualThemeTest {
    @Test
    fun returnsMaterial3Expressive() {
        assertEquals(
            PlayerVisualTheme.MATERIAL_3_EXPRESSIVE,
            PlayerVisualTheme.fromStoredValue("MATERIAL_3_EXPRESSIVE"),
        )
    }

    @Test
    fun frostedGlassMapsToFrostedGlass() {
        assertEquals(
            PlayerVisualTheme.FROSTED_GLASS,
            PlayerVisualTheme.fromStoredValue("FROSTED_GLASS"),
        )
        assertEquals(
            PlayerVisualTheme.FROSTED_GLASS,
            PlayerVisualTheme.fromStoredValue("frosted_glass"),
        )
        // Legacy fallback
        assertEquals(
            PlayerVisualTheme.FROSTED_GLASS,
            PlayerVisualTheme.fromStoredValue("LIQUID_GLASS"),
        )
    }

    @Test
    fun coverArtMapsToDuotone() {
        assertEquals(
            PlayerVisualTheme.DUOTONE,
            PlayerVisualTheme.fromStoredValue("COVER_ART"),
        )
        assertEquals(
            PlayerVisualTheme.DUOTONE,
            PlayerVisualTheme.fromStoredValue("COVER_ART_BLUR"),
        )
    }

    @Test
    fun newThemesRoundTripFromStoredValue() {
        PlayerVisualTheme.selectableEntries.forEach { theme ->
            assertEquals(theme, PlayerVisualTheme.fromStoredValue(theme.name))
        }
    }

    @Test
    fun unknownOrMissingThemeFallsBackSafely() {
        assertEquals(
            PlayerVisualTheme.MATERIAL_3_EXPRESSIVE,
            PlayerVisualTheme.fromStoredValue("future_theme"),
        )
        assertEquals(
            PlayerVisualTheme.MATERIAL_3_EXPRESSIVE,
            PlayerVisualTheme.fromStoredValue(null),
        )
    }

    @Test
    fun legacyThemesFallbackToMaterial3Expressive() {
        assertEquals(
            PlayerVisualTheme.MATERIAL_3_EXPRESSIVE,
            PlayerVisualTheme.fromStoredValue("OLED_MONO"),
        )
    }
}
