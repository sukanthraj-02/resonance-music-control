package com.sukanth.resonance.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = ResonanceTokens.Color.primary,
    onPrimary = Color.White,
    primaryContainer = ResonanceTokens.Color.primaryContainer,
    onPrimaryContainer = Color.White,
    inversePrimary = ResonanceTokens.Color.inversePrimary,
    secondary = ResonanceTokens.Color.secondary,
    onSecondary = Color.White,
    secondaryContainer = ResonanceTokens.Color.secondaryContainer,
    onSecondaryContainer = Color.White,
    tertiary = ResonanceTokens.Color.tertiary,
    onTertiary = Color.White,
    tertiaryContainer = ResonanceTokens.Color.tertiaryContainer,
    onTertiaryContainer = Color.White,
    background = ResonanceTokens.Color.background,
    onBackground = Color.White,
    surface = ResonanceTokens.Color.surface,
    onSurface = Color.White,
    surfaceVariant = ResonanceTokens.Color.surfaceVariant,
    onSurfaceVariant = Color.White,
    surfaceTint = ResonanceTokens.Color.surfaceTint,
    inverseSurface = ResonanceTokens.Color.inverseSurface,
    inverseOnSurface = Color.White,
    error = ResonanceTokens.Color.error,
    onError = Color.White,
    errorContainer = ResonanceTokens.Color.errorContainer,
    onErrorContainer = Color.White,
    outline = ResonanceTokens.Color.outline,
    outlineVariant = ResonanceTokens.Color.outlineVariant,
    scrim = ResonanceTokens.Color.scrim,
    surfaceDim = ResonanceTokens.Color.surfaceDim,
    surfaceBright = ResonanceTokens.Color.surfaceBright,
    surfaceContainerLowest = ResonanceTokens.Color.surfaceContainerLowest,
    surfaceContainerLow = ResonanceTokens.Color.surfaceContainerLow,
    surfaceContainer = ResonanceTokens.Color.surfaceContainer,
    surfaceContainerHigh = ResonanceTokens.Color.surfaceContainerHigh,
    surfaceContainerHighest = ResonanceTokens.Color.surfaceContainerHighest,
    primaryFixed = ResonanceTokens.Color.primaryFixed,
    primaryFixedDim = ResonanceTokens.Color.primaryFixedDim,
    onPrimaryFixed = Color.White,
    onPrimaryFixedVariant = Color.White,
    secondaryFixed = ResonanceTokens.Color.secondaryFixed,
    secondaryFixedDim = ResonanceTokens.Color.secondaryFixedDim,
    onSecondaryFixed = Color.White,
    onSecondaryFixedVariant = Color.White,
    tertiaryFixed = ResonanceTokens.Color.tertiaryFixed,
    tertiaryFixedDim = ResonanceTokens.Color.tertiaryFixedDim,
    onTertiaryFixed = Color.White,
    onTertiaryFixedVariant = Color.White,
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(ResonanceTokens.Shape.small),
    small = RoundedCornerShape(ResonanceTokens.Shape.medium),
    medium = RoundedCornerShape(ResonanceTokens.Shape.largeIncreased),
    large = RoundedCornerShape(ResonanceTokens.Shape.extraLarge),
    extraLarge = RoundedCornerShape(ResonanceTokens.Shape.extraLargeIncreased),
)

private val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.displayWeight,
        fontSize = ResonanceTokens.Type.displayLarge,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.displayWeight,
        fontSize = ResonanceTokens.Type.displayMedium,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.displayWeight,
        fontSize = ResonanceTokens.Type.displaySmall,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.headlineWeight,
        fontSize = ResonanceTokens.Type.headlineLarge,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.headlineWeight,
        fontSize = ResonanceTokens.Type.headlineMedium,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.headlineWeight,
        fontSize = ResonanceTokens.Type.headlineSmall,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.titleWeight,
        fontSize = ResonanceTokens.Type.titleLarge,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.titleWeight,
        fontSize = ResonanceTokens.Type.titleMedium,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.titleWeight,
        fontSize = ResonanceTokens.Type.titleSmall,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.bodyWeight,
        fontSize = ResonanceTokens.Type.bodyLarge,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.bodyWeight,
        fontSize = ResonanceTokens.Type.bodyMedium,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.bodyWeight,
        fontSize = ResonanceTokens.Type.bodySmall,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.labelWeight,
        fontSize = ResonanceTokens.Type.labelLarge,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.labelWeight,
        fontSize = ResonanceTokens.Type.labelMedium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = ResonanceTokens.Type.family,
        fontWeight = ResonanceTokens.Type.labelWeight,
        fontSize = ResonanceTokens.Type.labelSmall,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Dark-only Material 3 Expressive theme.
 *
 * The parameters remain for source compatibility. Material 3 1.4 contains the expressive motion
 * implementation, but both its factory and theme overload are internal. The public theme API is
 * therefore paired with [ResonanceTokens.Motion] for custom spatial motion until that API is
 * exported; no reflection or API-level-dependent behavior is used.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ResonanceTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content,
    )
}
