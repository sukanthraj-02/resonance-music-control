package com.sukanth.resonance.ui.lockscreen

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlin.math.max
import kotlin.math.min

/**
 * Frosted glass material constants.
 */
internal object FrostedGlass {
    /** Backdrop blur radius — softens the view just enough for that frosted look. */
    val BLUR_RADIUS = 24.dp

    /** Fallback blur for API < 31 where RenderEffect blur is unavailable. */
    val BLUR_RADIUS_LEGACY = 12.dp

    /** Content saturation multiplier when viewed through frosted glass. */
    const val SATURATION = 1.15f

    /** Specular highlight ambient intensity. */
    const val SPECULAR_AMBIENT = 0.08f

    /** Specular highlight peak intensity (direct light). */
    const val SPECULAR_PEAK = 0.10f

    /** Specular highlight radius as fraction of shape size. */
    const val SPECULAR_RADIUS = 0.50f

    /** Press-down scale for interactive elements. */
    const val PRESS_SCALE = 0.95f

    /** Glow intensity added on press. */
    const val PRESS_GLOW_ALPHA = 0.14f

    // Pre‑computed saturation color matrix.
    val SATURATION_MATRIX: ColorMatrix = run {
        val s = SATURATION
        val f = (1f - s) / 2f
        ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, f,
                0f, s, 0f, 0f, f,
                0f, s, 0f, 0f, f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}

/**
 * Applies the backdrop blur used by the Frosted canvas. A single hardware blur is
 * intentionally used here: stacking two full-screen RenderEffects caused dropped
 * frames on mid-range devices without a visible improvement once the frost texture
 * and tint are layered above it.
 */
internal fun Modifier.frostedGlassLayeredBlur(): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blur(FrostedGlass.BLUR_RADIUS)
    } else {
        blur(FrostedGlass.BLUR_RADIUS_LEGACY)
    }

/**
 * Draws a subtle noise/frost texture over the panel.
 * The noise bitmap is generated once per composition and reused.
 */
@Composable
internal fun Modifier.frostedGlassNoiseOverlay(): Modifier {
    // 8×8 noise bitmap with random grayscale values (≈6% opacity)
    val noiseBrush = remember<Brush> {
        val size = 8
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ALPHA_8)
        val rnd = java.util.Random()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val alpha = (rnd.nextFloat() * 255 * 0.06f).toInt()
                bitmap.setPixel(x, y, android.graphics.Color.argb(alpha, 0, 0, 0))
            }
        }
        ShaderBrush(
            ImageShader(
                image = bitmap.asImageBitmap(),
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            ),
        )
    }
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = noiseBrush,
            size = size,
            alpha = 1f,
        )
    }
}

/**
 * Enhanced specular highlight that drifts sinusoidally over time.
 * No sensor required – provides a gentle animated sheen.
 */
@Composable
internal fun Modifier.frostedGlassSpecularEnhanced(
    coverPrimary: Color = Color.White,
): Modifier {
    val infiniteTransition = rememberInfiniteTransition()
    val drift by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "specular drift",
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val halfW = w / 2f
        val halfH = h / 2f
        val offsetX = halfW * drift
        val offsetY = halfH * 0.32f // fixed vertical fraction
        val radius = min(w, h) * FrostedGlass.SPECULAR_RADIUS
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = FrostedGlass.SPECULAR_PEAK),
                    0.45f to Color.White.copy(alpha = FrostedGlass.SPECULAR_AMBIENT * 0.4f),
                    0.75f to coverPrimary.copy(alpha = FrostedGlass.SPECULAR_AMBIENT * 0.12f),
                    1f to Color.Transparent,
                ),
                center = Offset(halfW + offsetX, offsetY),
                radius = radius,
            ),
            radius = radius,
            center = Offset(halfW + offsetX, offsetY),
        )
    }
}

/**
 * Draws a thin inner rim (≈2 % of the shortest side) with a soft outer matte edge.
 */
internal fun Modifier.frostedGlassRimEnhanced(
    edgeColor: Color = Color.White,
    intensity: Float = 1f,
): Modifier = this.drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    val rimThickness = min(w, h) * 0.02f // thinner rim (2%)
    // Top edge – brighter
    val topAlpha = (0.04f + 0.06f * intensity).coerceAtMost(0.10f)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to edgeColor.copy(alpha = topAlpha),
                0.55f to edgeColor.copy(alpha = topAlpha * 0.30f),
                1f to Color.Transparent,
            ),
            startY = 0f,
            endY = rimThickness,
        ),
        topLeft = Offset.Zero,
        size = Size(w, rimThickness),
    )
    // Outer matte edge (low alpha)
    val outerAlpha = 0.03f * intensity
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to edgeColor.copy(alpha = outerAlpha),
                1f to Color.Transparent,
            ),
            startY = 0f,
            endY = rimThickness * 1.5f,
        ),
        topLeft = Offset.Zero,
        size = Size(w, rimThickness * 1.5f),
    )
}

/**
 * Applies saturation boost to content drawn through this modifier,
 * simulating the vibrancy of viewing through frosted glass.
 */
internal fun Modifier.frostedGlassSaturation(): Modifier = this.drawWithContent {
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(FrostedGlass.SATURATION_MATRIX)
    }
    drawContext.canvas.saveLayer(
        Rect(0f, 0f, size.width, size.height),
        paint,
    )
    drawContent()
    drawContext.canvas.restore()
}

/**
 * Subtle colored light bleed that makes frosted glass feel connected to the
 * artwork behind it. Broad, low‑alpha — reads as refracted light, not a solid
 * card.
 */
internal fun Modifier.frostedGlassAmbientTint(
    coverPrimary: Color,
    coverSecondary: Color,
): Modifier = this.drawWithContent {
    drawContent()
    val radius = max(size.width, size.height) * 0.90f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coverPrimary.copy(alpha = 0.10f),
                coverPrimary.copy(alpha = 0.03f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.10f, size.height * 0.10f),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width * 0.10f, size.height * 0.10f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coverSecondary.copy(alpha = 0.08f),
                coverSecondary.copy(alpha = 0.02f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.90f, size.height * 0.90f),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width * 0.90f, size.height * 0.90f),
    )
}

/**
 * Frosted glass interactive press: scales down, animates elevation, and adds
 * an inner shadow when pressed.
 */
@Composable
internal fun Modifier.frostedGlassInteractiveEnhanced(
    interactionSource: MutableInteractionSource,
    pressScale: Float = FrostedGlass.PRESS_SCALE,
    enabled: Boolean = true,
    glowColor: Color = Color.White,
    shape: Shape? = null,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    var pressPosition by remember(interactionSource) { mutableStateOf(Offset.Unspecified) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressPosition = interaction.pressPosition
                is PressInteraction.Release, is PressInteraction.Cancel -> pressPosition = Offset.Unspecified
            }
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed && enabled) 0.85f else 0.60f,
            stiffness = if (isPressed && enabled) 650f else 350f,
        ),
        label = "fg press scale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (isPressed && enabled) {
                val center = if (pressPosition.x.isFinite() && pressPosition.y.isFinite()) {
                    pressPosition
                } else {
                    Offset(size.width / 2f, size.height / 2f)
                }
                val radius = max(size.width, size.height) * 0.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Transparent),
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
}

/**
 * Full frosted glass panel effect. The expensive blur is applied once to the
 * full-screen backdrop rather than separately on every panel; panels only add
 * their lightweight frost texture and rim. Keeping the tab interior flat avoids
 * the distracting glow pools that can appear between controls.
 */
@Composable
internal fun Modifier.frostedGlassPanelEffects(
    coverPrimary: Color,
    coverSecondary: Color,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val material = this
        .frostedGlassNoiseOverlay()
        .frostedGlassRimEnhanced(edgeColor = Color.White, intensity = 1f)
    return if (interactionSource == null) {
        material
    } else {
        material.frostedGlassInteractiveEnhanced(interactionSource = interactionSource)
    }
}
