package com.sukanth.resonance.lockscreen

enum class PlayerVisualTheme(
    val displayName: String,
    val description: String,
) {
    MATERIAL_3_EXPRESSIVE(
        displayName = "Material 3 Expressive",
        description = "Emotion-led media controls with bold color, adaptive shapes, and physics-based motion",
    ),
    FROSTED_GLASS(
        displayName = "Frosted Glass",
        description = "Airy frosted-glass widgets with translucent panels, soft specular rims, and an accent play orb",
    ),
    HI_FI_STUDIO(
        displayName = "Hi-Fi Studio",
        description = "Vintage audio gear with a brushed chrome deck, warm amber reads, and analog VU meters",
    ),
    DUOTONE(
        displayName = "Duotone Canvas",
        description = "Editorial two-tone artwork canvas with gradient washes, bold type, and rounded transport pills",
    );

    companion object {
        val selectableEntries: List<PlayerVisualTheme> =
            listOf(MATERIAL_3_EXPRESSIVE, FROSTED_GLASS, HI_FI_STUDIO, DUOTONE)

        fun fromStoredValue(value: String?): PlayerVisualTheme = when (value?.trim()?.uppercase()) {
            "FROSTED_GLASS", "LIQUID_GLASS", "IOS_GLASS" -> FROSTED_GLASS
            "COVER_ART", "COVER_ART_BLUR" -> DUOTONE
            "HI_FI_STUDIO" -> HI_FI_STUDIO
            "DUOTONE" -> DUOTONE
            else -> MATERIAL_3_EXPRESSIVE
        }
    }
}
