package com.sukanth.resonance.ui.lockscreen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sukanth.resonance.lockscreen.PlayerVisualTheme

@Immutable
internal data class PlayerVisualStyle(
    val cardShape: Shape,
    val artworkShape: Shape,
    val transportShape: Shape,
    val playShape: Shape,
    val cardBrush: Brush,
    val cardBorder: Brush,
    val artworkBorder: Brush,
    val borderWidth: Dp,
    val shadowColor: Color,
    val foreground: Color,
    val secondaryForeground: Color,
    val badgeContainer: Color,
    val badgeContent: Color,
    val primaryContainer: Color,
    val primaryContent: Color,
    val secondaryContainer: Color,
    val secondaryContent: Color,
    val tertiaryContainer: Color,
    val tertiaryContent: Color,
    val outline: Color,
    val controlContainer: Color,
    val sliderInactive: Color,
    val coverPrimary: Color,
    val coverSecondary: Color,
)

/**
 * The single visual language used by Resonance Material Control.
 *
 * Album art chooses the expressive hue keys, while MaterialTheme supplies the
 * semantic role relationships and contrast-aware on-colors.
 */
@Composable
internal fun rememberPlayerVisualStyle(
    theme: PlayerVisualTheme,
    coverPrimary: Color,
    coverSecondary: Color,
): PlayerVisualStyle {
    val colors = MaterialTheme.colorScheme
    return remember(theme, colors, coverPrimary, coverSecondary) {
        val expressivePrimary = coverPrimary.boldAccent(saturation = 1.24f, lift = 0.10f)
        val expressiveSecondary = coverSecondary.boldAccent(saturation = 1.20f, lift = 0.08f)
        val primaryKey = colors.primary
            .blendedWith(expressivePrimary, 0.64f)
            .copy(alpha = 1f)
        val secondaryKey = colors.secondary
            .blendedWith(expressiveSecondary, 0.64f)
            .copy(alpha = 1f)
        val primaryContainer = primaryKey.lifted(0.05f)
        val secondaryContainer = colors.secondaryContainer
            .blendedWith(secondaryKey.scaled(0.78f).lifted(0.06f), 0.54f)
            .copy(alpha = 1f)
        val tertiaryContainer = colors.tertiaryContainer
            .blendedWith(
                colors.tertiary
                    .blendedWith(expressiveSecondary, 0.56f)
                    .scaled(0.76f)
                    .lifted(0.06f),
                0.46f,
            )
            .copy(alpha = 1f)
        val surfaceContainer = colors.surfaceContainer
            .blendedWith(primaryKey.scaled(0.14f), 0.16f)
            .blendedWith(secondaryKey.scaled(0.10f), 0.08f)
            .copy(alpha = 1f)

        if (theme == PlayerVisualTheme.FROSTED_GLASS) {
            // Frosted Glass is deliberately cool and neutral. Album art appears as
            // refracted light at the edge, not as a second Duotone-style colour wash.
            val glassCardBrush = Brush.verticalGradient(
                listOf(
                    Color(0xFFC7D9E8).copy(alpha = 0.16f),
                    Color(0xFF91A9BC).copy(alpha = 0.13f),
                    Color(0xFF536B7E).copy(alpha = 0.12f),
                    coverPrimary.copy(alpha = 0.055f),
                ),
            )
            val glassBorderBrush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.52f),
                    Color(0xFFC1D7EB).copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.11f),
                    coverSecondary.copy(alpha = 0.075f),
                ),
            )
            PlayerVisualStyle(
                cardShape = RoundedCornerShape(30.dp),
                artworkShape = RoundedCornerShape(26.dp),
                transportShape = RoundedCornerShape(24.dp),
                playShape = RoundedCornerShape(50.dp),
                cardBrush = glassCardBrush,
                cardBorder = glassBorderBrush,
                artworkBorder = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                borderWidth = 1.dp,
                shadowColor = Color(0xFF05090F).copy(alpha = 0.58f),
                foreground = Color.White,
                secondaryForeground = Color.White.copy(alpha = 0.85f),
                badgeContainer = Color.Black.copy(alpha = 0.45f),
                badgeContent = Color.White,
                primaryContainer = primaryContainer,
                primaryContent = Color.White,
                secondaryContainer = Color(0xFF89A7BF).copy(alpha = 0.18f),
                secondaryContent = Color.White,
                tertiaryContainer = tertiaryContainer,
                tertiaryContent = Color.White,
                outline = Color.White.copy(alpha = 0.28f),
                controlContainer = Color(0xFF101822).copy(alpha = 0.62f),
                sliderInactive = Color.White.copy(alpha = 0.28f),
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
            )
        } else if (theme == PlayerVisualTheme.HI_FI_STUDIO) {
            // Vintage hi-fi: retain the dark chassis while tuning every metal and LED accent
            // to the current record sleeve, like the adaptive Material 3 treatment.
            val hifiPrimary = coverPrimary.boldAccent(saturation = 1.12f, lift = 0.10f)
            val hifiSecondary = coverSecondary.boldAccent(saturation = 1.08f, lift = 0.08f)
            val chromeBrush = Brush.verticalGradient(
                listOf(
                    Color(0xFFE6E2DC).copy(alpha = 0.85f),
                    hifiPrimary.lifted(0.52f).copy(alpha = 0.52f),
                    hifiSecondary.scaled(0.38f).copy(alpha = 0.68f),
                ),
            )
            val chassisBrush = Brush.verticalGradient(
                listOf(
                    hifiPrimary.scaled(0.20f).blendedWith(Color(0xFF191310), 0.62f),
                    hifiSecondary.scaled(0.16f).blendedWith(Color(0xFF0D0B0A), 0.76f),
                ),
            )
            PlayerVisualStyle(
                cardShape = RoundedCornerShape(18.dp),
                artworkShape = RoundedCornerShape(10.dp),
                transportShape = RoundedCornerShape(50.dp),
                playShape = RoundedCornerShape(50.dp),
                cardBrush = chassisBrush,
                cardBorder = chromeBrush,
                artworkBorder = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.38f),
                        hifiPrimary.lifted(0.38f).copy(alpha = 0.34f),
                        hifiSecondary.scaled(0.42f).copy(alpha = 0.28f),
                    ),
                ),
                borderWidth = 1.5.dp,
                shadowColor = hifiPrimary.scaled(0.24f).copy(alpha = 0.68f),
                foreground = Color(0xFFF3EBDC),
                secondaryForeground = hifiPrimary.lifted(0.28f),
                badgeContainer = Color(0xFF1C1714),
                badgeContent = hifiPrimary.lifted(0.34f),
                primaryContainer = hifiPrimary,
                primaryContent = Color(0xFF181308),
                secondaryContainer = hifiSecondary.scaled(0.58f),
                secondaryContent = Color(0xFFF3EBDC),
                tertiaryContainer = hifiSecondary.scaled(0.72f),
                tertiaryContent = Color(0xFFF3EBDC),
                outline = hifiSecondary.lifted(0.18f).copy(alpha = 0.72f),
                controlContainer = hifiPrimary.scaled(0.16f).blendedWith(Color(0xFF1B1612), 0.78f),
                sliderInactive = hifiSecondary.lifted(0.16f).copy(alpha = 0.54f),
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
            )
        } else if (theme == PlayerVisualTheme.DUOTONE) {
            // Spotify-style duotone canvas: bold two-hue gradient washes + white type.
            val duoBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to coverPrimary,
                    0.55f to coverPrimary.blendedWith(coverSecondary, 0.55f),
                    1.00f to coverSecondary,
                ),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(1600f, 1600f),
            )
            PlayerVisualStyle(
                cardShape = RoundedCornerShape(30.dp),
                artworkShape = RoundedCornerShape(24.dp),
                transportShape = RoundedCornerShape(50.dp),
                playShape = RoundedCornerShape(50.dp),
                cardBrush = duoBrush,
                cardBorder = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f),
                    ),
                ),
                artworkBorder = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                borderWidth = 1.dp,
                shadowColor = coverPrimary.scaled(0.4f).copy(alpha = 0.5f),
                foreground = Color.White,
                secondaryForeground = Color.White.copy(alpha = 0.82f),
                badgeContainer = Color.Black.copy(alpha = 0.40f),
                badgeContent = Color.White,
                primaryContainer = coverPrimary.boldAccent(saturation = 1.15f, lift = 0.05f),
                primaryContent = Color.White,
                secondaryContainer = coverSecondary.boldAccent(saturation = 1.10f, lift = 0.05f).copy(alpha = 0.55f),
                secondaryContent = Color.White,
                tertiaryContainer = coverSecondary.boldAccent(saturation = 1.0f, lift = 0.02f),
                tertiaryContent = Color.White,
                outline = Color.White.copy(alpha = 0.35f),
                controlContainer = Color(0xFF14100F),
                sliderInactive = Color.White.copy(alpha = 0.34f),
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
            )
        } else {
            PlayerVisualStyle(
                cardShape = RoundedCornerShape(30.dp),
                artworkShape = RoundedCornerShape(36.dp),
                transportShape = RoundedCornerShape(26.dp),
                playShape = RoundedCornerShape(30.dp),
                cardBrush = SolidColor(surfaceContainer),
                cardBorder = SolidColor(Color.Transparent),
                artworkBorder = SolidColor(Color.Transparent),
                borderWidth = 0.dp,
                shadowColor = Color.Transparent,
                // Keep the player hierarchy white-on-color; surfaces and artwork provide the tonal
                // separation instead of dark text.
                foreground = Color.White,
                secondaryForeground = Color.White,
                badgeContainer = secondaryContainer,
                badgeContent = Color.White,
                primaryContainer = primaryContainer,
                primaryContent = Color.White,
                secondaryContainer = secondaryContainer,
                secondaryContent = Color.White,
                tertiaryContainer = tertiaryContainer,
                tertiaryContent = Color.White,
                outline = colors.outlineVariant,
                controlContainer = surfaceContainer,
                sliderInactive = colors.onSurfaceVariant.copy(alpha = 0.38f),
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
            )
        }
    }
}

private fun Color.scaled(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.lifted(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

private fun Color.blendedWith(other: Color, otherFraction: Float): Color {
    val fraction = otherFraction.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = alpha + (other.alpha - alpha) * fraction,
    )
}

private fun Color.boldAccent(saturation: Float, lift: Float): Color {
    val midpoint = (red + green + blue) / 3f
    return Color(
        red = (midpoint + (red - midpoint) * saturation).coerceIn(0f, 1f),
        green = (midpoint + (green - midpoint) * saturation).coerceIn(0f, 1f),
        blue = (midpoint + (blue - midpoint) * saturation).coerceIn(0f, 1f),
        alpha = alpha,
    ).lifted(lift)
}
