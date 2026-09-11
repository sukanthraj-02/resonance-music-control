package com.sukanth.resonance.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Single source of truth for the Material 3 Expressive Resonance visual language. */
object ResonanceTokens {
    object Color {
        // Signature colors retained for call-site compatibility and artwork-led accents.
        val ink = ComposeColor(0xFF090B12)
        val canvas = ComposeColor(0xFF111522)
        val lavender = ComposeColor(0xFFB9C7FF)
        val lilac = ComposeColor(0xFFE3B9FF)
        val aqua = ComposeColor(0xFF9DEBDD)
        val blue = ComposeColor(0xFF6D9BFF)
        val ivory = ComposeColor(0xFFFFF9F2)
        val neutral = ComposeColor(0xFFAAB1C3)

        // Material semantic roles. Components should consume these through MaterialTheme.
        val primary = lavender
        val onPrimary = ComposeColor(0xFF202A4A)
        val primaryContainer = ComposeColor(0xFF3A4770)
        val onPrimaryContainer = ComposeColor(0xFFE8ECFF)
        val inversePrimary = ComposeColor(0xFF50619A)

        val secondary = lilac
        val onSecondary = ComposeColor(0xFF35223F)
        val secondaryContainer = ComposeColor(0xFF5A3F73)
        val onSecondaryContainer = ComposeColor(0xFFF5DEFF)

        val tertiary = aqua
        val onTertiary = ComposeColor(0xFF063832)
        val tertiaryContainer = ComposeColor(0xFF1D5D5D)
        val onTertiaryContainer = ComposeColor(0xFFB5FFF0)

        val error = ComposeColor(0xFFFFB4AB)
        val onError = ComposeColor(0xFF690005)
        val errorContainer = ComposeColor(0xFF93000A)
        val onErrorContainer = ComposeColor(0xFFFFDAD6)

        val background = ink
        val onBackground = ivory
        val surface = canvas
        val onSurface = ivory
        val surfaceVariant = ComposeColor(0xFF424758)
        val onSurfaceVariant = neutral
        val surfaceTint = primary
        val inverseSurface = ComposeColor(0xFFE5E1EA)
        val inverseOnSurface = ComposeColor(0xFF2E3038)
        val outline = ComposeColor(0xFF77819A)
        val outlineVariant = ComposeColor(0xFF424758)
        val scrim = ComposeColor.Black

        val surfaceDim = ComposeColor(0xFF0D0F17)
        val surfaceBright = ComposeColor(0xFF343744)
        val surfaceContainerLowest = ink
        val surfaceContainerLow = ComposeColor(0xFF151925)
        val surfaceContainer = ComposeColor(0xFF1A2030)
        val surfaceContainerHigh = ComposeColor(0xFF252C40)
        val surfaceContainerHighest = ComposeColor(0xFF313A52)

        // Fixed roles deliberately do not change between light and dark environments.
        val primaryFixed = ComposeColor(0xFFDCE4FF)
        val primaryFixedDim = primary
        val onPrimaryFixed = ComposeColor(0xFF101B3B)
        val onPrimaryFixedVariant = ComposeColor(0xFF384873)
        val secondaryFixed = ComposeColor(0xFFF5DAFF)
        val secondaryFixedDim = secondary
        val onSecondaryFixed = ComposeColor(0xFF281332)
        val onSecondaryFixedVariant = ComposeColor(0xFF5D3B6B)
        val tertiaryFixed = ComposeColor(0xFFB5FFF0)
        val tertiaryFixedDim = tertiary
        val onTertiaryFixed = ComposeColor(0xFF00201C)
        val onTertiaryFixedVariant = ComposeColor(0xFF16534C)

        // Legacy alias retained for existing screens.
        val surfaceHigh = surfaceContainerHigh
    }

    object Space {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val display = 48.dp
    }

    object Shape {
        // Official Material 3 rounded-corner scale.
        val none = 0.dp
        val extraSmall = 4.dp
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val largeIncreased = 20.dp
        val extraLarge = 28.dp
        val extraLargeIncreased = 32.dp
        val extraExtraLarge = 48.dp
        val full = 999.dp

        // Compatibility names used by the existing UI.
        val compact = medium
        val control = large
        val card = extraLarge
        val hero = extraLargeIncreased
        val expressive = extraExtraLarge
    }

    object Type {
        // SansSerif resolves to Android's platform sans family without bundling a font.
        val family = FontFamily.SansSerif
        val displayWeight = FontWeight.Medium
        val headlineWeight = FontWeight.SemiBold
        val titleWeight = FontWeight.SemiBold
        val bodyWeight = FontWeight.Normal
        val labelWeight = FontWeight.SemiBold

        val displayLarge = 57.sp
        val displayMedium = 45.sp
        val displaySmall = 36.sp
        val headlineLarge = 32.sp
        val headlineMedium = 28.sp
        val headlineSmall = 24.sp
        val titleLarge = 22.sp
        val titleMedium = 16.sp
        val titleSmall = 14.sp
        val bodyLarge = 16.sp
        val bodyMedium = 14.sp
        val bodySmall = 12.sp
        val labelLarge = 14.sp
        val labelMedium = 12.sp
        val labelSmall = 11.sp
    }

    object Motion {
        const val effectsFastMs = 100
        const val effectsDefaultMs = 200
        const val effectsSlowMs = 300

        // Material 3 Expressive Fast Spatial motion. Keep these values named so every
        // interactive control uses the same physical response.
        const val fastSpatialDamping = 0.6f
        const val fastSpatialStiffness = 800f
        const val fastEffectsDamping = 1.0f
        const val fastEffectsStiffness = 3800f
        const val expressiveRippleDurationMs = 24

        // Compatibility durations used by existing custom transitions.
        const val colorChangeMs = 260
        const val contentEnterMs = 280
        const val contentExitMs = 180

        val standard = tween<Float>(
            durationMillis = contentEnterMs,
            easing = FastOutSlowInEasing,
        )
        val fastSpatial = spring<Float>(
            dampingRatio = fastSpatialDamping,
            stiffness = fastSpatialStiffness,
        )
        val fastSpatialDp = spring<Dp>(
            dampingRatio = fastSpatialDamping,
            stiffness = fastSpatialStiffness,
        )
        val fastEffects = spring<ComposeColor>(
            dampingRatio = fastEffectsDamping,
            stiffness = fastEffectsStiffness,
        )
        val expressiveSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
        val restrainedSpatial = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
        val slowSpatial = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        )

        // Apple Liquid Glass springs — response + dampingFraction model from WWDC 2025.
        // Calibrated to match SwiftUI Spring behaviour in Compose's spring DSL.

        /** Interactive drag: response=0.15s, dampingFraction=0.86. Near-instant, low overshoot. */
        val liquidInteractive = spring<Float>(
            dampingRatio = 0.86f,
            stiffness = 280f,
        )

        /** Button press: response=0.3s, dampingFraction=0.6. Snappy with subtle bounce. */
        val liquidButtonPress = spring<Float>(
            dampingRatio = 0.6f,
            stiffness = 175f,
        )

        /** Sheet morph: response=0.4s, dampingFraction=0.6. Smooth expansion/contraction. */
        val liquidSheetMorph = spring<Float>(
            dampingRatio = 0.6f,
            stiffness = 97f,
        )

        /** Snappy tap: response=0.2s, very low bounce. */
        val liquidSnappy = spring<Float>(
            dampingRatio = 0.85f,
            stiffness = 350f,
        )

        /** Playful bounce: .bouncy preset. */
        val liquidBouncy = spring<Float>(
            dampingRatio = 0.45f,
            stiffness = 180f,
        )
    }
}
