package com.sukanth.resonance.ui.lockscreen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How a theme presents its playlist: a side panel on wide canvases, otherwise a bottom sheet. */
internal enum class PlaylistPresentation {
    BOTTOM_SHEET,
    SIDE_PANEL,
}

/** Material-style width class for the lock-screen canvas. */
internal enum class PlayerWidthTier {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

/** Vertical room class; drives how much airy spacing a theme can afford. */
internal enum class PlayerHeightTier {
    SHORT,
    REGULAR,
    TALL,
}

/**
 * A single, shared description of the available lock-screen canvas.
 *
 * Every theme reads the same metrics so that a tall 20:9 phone, a short 16:9 phone,
 * a tablet, an unfolded foldable, landscape, and a split-screen window all resolve
 * to a deliberate composition instead of a stretched or clipped one.
 */
internal data class PlayerLayoutMetrics(
    val width: Dp,
    val height: Dp,
    val widthTier: PlayerWidthTier,
    val heightTier: PlayerHeightTier,
    val isLandscape: Boolean,
    val isTablet: Boolean,
    /** True for tall, narrow canvases where vertical spacing must be conservative. */
    val tight: Boolean,
    /** Generous vertical room; themes can breathe. */
    val roomy: Boolean,
    /** Two-pane composition for the redesigned themes (landscape or large tablets). */
    val split: Boolean,
    /** Legacy two-column routing retained for the Material 3 Expressive layout. */
    val wide: Boolean,
    /** Legacy compact flag retained for the Material 3 Expressive layout. */
    val compact: Boolean,
    val contentMaxWidth: Dp,
    val contentSideInset: Dp,
    val screenPadding: Dp,
    /** Global multiplier for type sizes (roughly 0.90f … 1.20f). */
    val typeScale: Float,
    /** Global multiplier for vertical rhythm. */
    val spacingScale: Float,
    /** Suggest artwork side as a fraction of the shortest axis. */
    val artworkFraction: Float,
) {
    val playlistPresentation: PlaylistPresentation
        get() = if (wide) PlaylistPresentation.SIDE_PANEL else PlaylistPresentation.BOTTOM_SHEET
}

/**
 * Derives the layout metrics from the canvas size.
 *
 * Kept as a pure function so the breakpoints are deterministic and easy to reason
 * about across device classes:
 *  - phone portrait  → single column, tight/regular/tall vertical rhythm;
 *  - phone landscape → two-pane, conservative rhythm on very short canvases;
 *  - tablet/foldable → two-pane on wider canvases, scaled-up type and artwork;
 *  - split-screen    → falls back to whatever class the resized window lands in.
 */
internal fun playerLayoutMetrics(width: Dp, height: Dp): PlayerLayoutMetrics {
    val isLandscape = width > height
    val shortest = minOf(width, height)
    val isTablet = shortest >= 600.dp
    val widthTier = when {
        width < 600.dp -> PlayerWidthTier.COMPACT
        width < 840.dp -> PlayerWidthTier.MEDIUM
        else -> PlayerWidthTier.EXPANDED
    }
    val heightTier = when {
        height < 600.dp -> PlayerHeightTier.SHORT
        height < 900.dp -> PlayerHeightTier.REGULAR
        else -> PlayerHeightTier.TALL
    }
    val tight = heightTier == PlayerHeightTier.SHORT || isLandscape
    val roomy = heightTier == PlayerHeightTier.TALL && !isLandscape
    // Two-pane for landscape and for genuinely wide tablets/foldables; portrait
    // phones and narrow foldables keep the familiar single column.
    val wide = isLandscape && width >= 480.dp
    val split = wide || (isTablet && width >= 700.dp)
    val contentMaxWidth = when {
        widthTier == PlayerWidthTier.EXPANDED -> minOf(width, 1040.dp)
        widthTier == PlayerWidthTier.MEDIUM -> minOf(width, if (split) 920.dp else 720.dp)
        else -> minOf(width, 560.dp)
    }
    val contentSideInset = ((width - contentMaxWidth) / 2).coerceAtLeast(0.dp)
    val screenPadding = if (widthTier == PlayerWidthTier.COMPACT) 16.dp else 24.dp
    val typeScale = when {
        shortest < 360.dp -> 0.90f
        shortest < 400.dp -> 0.95f
        shortest < 480.dp -> 1.0f
        shortest < 600.dp -> 1.06f
        shortest < 840.dp -> 1.12f
        else -> 1.20f
    }
    val spacingScale = when {
        shortest < 360.dp -> 0.86f
        shortest < 480.dp -> 1.0f
        shortest < 600.dp -> 1.08f
        shortest < 840.dp -> 1.16f
        else -> 1.28f
    }
    val artworkFraction = when {
        isLandscape -> 0.94f
        shortest < 400.dp -> 0.78f
        roomy -> 0.72f
        else -> 0.84f
    }
    return PlayerLayoutMetrics(
        width = width,
        height = height,
        widthTier = widthTier,
        heightTier = heightTier,
        isLandscape = isLandscape,
        isTablet = isTablet,
        tight = tight,
        roomy = roomy,
        split = split,
        wide = wide,
        compact = height < 760.dp || isLandscape,
        contentMaxWidth = contentMaxWidth,
        contentSideInset = contentSideInset,
        screenPadding = screenPadding,
        typeScale = typeScale,
        spacingScale = spacingScale,
        artworkFraction = artworkFraction,
    )
}

/** Scales a dp value by the theme spacing multiplier. */
internal fun Dp.rhythm(scale: Float): Dp = this * scale

/**
 * Reserves an area for a square artwork panel and sizes it against both axes.
 *
 * The box owns the available space (typically via `Modifier.weight(1f)`), and the
 * square is the smallest of the width budget, the full height budget, and an
 * absolute cap — so it can never clip on short screens and never balloons on tall
 * ones. This is what lets one composition serve phones through tablets.
 */
@Composable
internal fun AdaptiveSquareArtwork(
    widthFraction: Float,
    modifier: Modifier = Modifier,
    maxArtwork: Dp = 460.dp,
    minArtwork: Dp = 112.dp,
    content: @Composable (Dp) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth * widthFraction, maxHeight, maxArtwork)
            .coerceAtLeast(minOf(maxWidth, minArtwork))
        content(side)
    }
}
