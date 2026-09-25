package com.sukanth.resonance.ui.lockscreen

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import com.sukanth.resonance.lockscreen.ExternalMediaState
import com.sukanth.resonance.lockscreen.PlaylistEntry
import com.sukanth.resonance.lockscreen.PlayerVisualTheme
import com.sukanth.resonance.ui.icons.ResonanceIcons
import com.sukanth.resonance.ui.theme.ResonanceTokens
import com.sukanth.resonance.util.formatPlaybackTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class PlaylistPresentation {
    BOTTOM_SHEET,
    SIDE_PANEL,
}

/**
 * Width and height are both important on a lock screen: a landscape phone has a wide
 * canvas but very little vertical room, while a tablet needs a denser, more deliberate
 * composition than a stretched phone column.
 */
private data class ResponsivePlayerLayout(
    val wide: Boolean,
    val compact: Boolean,
    val contentMaxWidth: Dp,
    val contentSideInset: Dp,
    val showClock: Boolean,
    val playlistPresentation: PlaylistPresentation,
)

/** Avoid continuously redrawing decorative motion on devices that ask for less work. */
@Composable
private fun rememberAmbientMotionEnabled(isPlaying: Boolean): Boolean {
    val context = LocalContext.current
    return remember(context, isPlaying) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val systemAnimationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            true
        }
        isPlaying &&
            systemAnimationsEnabled &&
            activityManager?.isLowRamDevice != true &&
            powerManager?.isPowerSaveMode != true
    }
}

/**
 * A translucent lock-screen surface. Android's real keyguard remains visible behind this content;
 * only the artwork and media controls are drawn by the app.
 */
@Composable
fun LockScreenPlayerScreen(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    isExiting: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaylistItemSelected: (PlaylistEntry) -> Unit,
    onOpenSourceApp: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onUnlock: () -> Unit,
    onClose: () -> Unit,
) {
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    var playlistVisible by remember { mutableStateOf(false) }

    val coverPrimary by animateColorAsState(
        targetValue = Color(state.artworkPrimaryArgb),
        animationSpec = tween(durationMillis = ResonanceTokens.Motion.colorChangeMs, easing = FastOutSlowInEasing),
        label = "cover primary",
    )
    val coverSecondary by animateColorAsState(
        targetValue = Color(state.artworkSecondaryArgb),
        animationSpec = tween(durationMillis = ResonanceTokens.Motion.colorChangeMs + 30, easing = FastOutSlowInEasing),
        label = "cover secondary",
    )
    // Album art selects the expressive hue keys while Material roles preserve contrast.
    val visualStyle = rememberPlayerVisualStyle(
        theme = visualTheme,
        coverPrimary = coverPrimary,
        coverSecondary = coverSecondary,
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (isExiting) 180 else 90,
            easing = FastOutSlowInEasing,
        ),
        label = "player alpha",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isExiting) 0.97f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "player scale",
    )
    val contentOffsetDp by animateFloatAsState(
        targetValue = if (isExiting) 28f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "player offset",
    )
    val density = LocalDensity.current
    val opaqueBackground = remember(coverPrimary, coverSecondary) {
        SolidColor(coverPrimary.scaledForBackground(0.26f))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDrag = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        verticalDrag += amount
                    },
                    onDragEnd = {
                        when {
                            verticalDrag < -110f -> onUnlock()
                            verticalDrag > 140f -> onClose()
                        }
                        verticalDrag = 0f
                    },
                )
            },
    ) {
        // The background may use the whole display, but player controls must react to both axes.
        // Tablets/foldables and landscape phones use a two-column player; portrait phones retain
        // the compact vertical composition. This avoids both stretched tabs and dead space.
        val isLandscape = maxWidth > maxHeight
        val wideLayout = maxWidth >= 600.dp || (isLandscape && maxWidth >= 480.dp)
        val compact =
            maxHeight < (if (visualTheme == PlayerVisualTheme.FROSTED_GLASS) 840.dp else 760.dp) ||
                isLandscape
        val contentMaxWidth = minOf(maxWidth, if (wideLayout) 960.dp else 560.dp)
        val contentSideInset = ((maxWidth - contentMaxWidth) / 2).coerceAtLeast(0.dp)
        val responsiveLayout = ResponsivePlayerLayout(
            wide = wideLayout,
            compact = compact,
            contentMaxWidth = contentMaxWidth,
            contentSideInset = contentSideInset,
            // A clock is useful on a tablet and phone portrait. On short landscape displays it
            // competes with music controls, so the artwork/control split gets that room instead.
            showClock = !isLandscape || maxHeight >= 600.dp,
            playlistPresentation = if (wideLayout) {
                PlaylistPresentation.SIDE_PANEL
            } else {
                PlaylistPresentation.BOTTOM_SHEET
            },
        )
        ThemeBackdrop(
            state = state,
            visualTheme = visualTheme,
            background = opaqueBackground,
            coverPrimary = coverPrimary,
            coverSecondary = coverSecondary,
            alpha = contentAlpha,
        )
        ThemePlayerLayout(
            state = state,
            visualTheme = visualTheme,
            style = visualStyle,
            responsiveLayout = responsiveLayout,
            modifier = Modifier
                .fillMaxSize()
                // Status/navigation padding covers ordinary phones; cutout padding also protects
                // landscape camera islands and asymmetric foldable displays.
                .displayCutoutPadding()
                .statusBarsPadding()
                .navigationBarsPadding()
                // Reserve a clean bottom rail for the playlist split button so it never
                // overlaps the volume surface or the system gesture area.
                .padding(bottom = if (wideLayout) 64.dp else 76.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                    translationY = with(density) {
                        contentOffsetDp.dp.toPx()
                    }
                },
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onSeek = onSeek,
            onVolumeChange = onVolumeChange,
            onLike = onLike,
            onShuffle = onShuffle,
        )
        val playlistBottomPadding = 10.dp
        if (playlistVisible) {
            PlaylistOverlay(
                state = state,
                style = visualStyle,
                frostedGlass = visualTheme == PlayerVisualTheme.FROSTED_GLASS,
                presentation = responsiveLayout.playlistPresentation,
                onDismiss = { playlistVisible = false },
                onSelect = { entry ->
                    onPlaylistItemSelected(entry)
                    playlistVisible = false
                },
                onOpenSourceApp = onOpenSourceApp,
            )
        }
        PlaylistSplitButton(
            style = visualStyle,
            expanded = playlistVisible,
            frostedGlass = visualTheme == PlayerVisualTheme.FROSTED_GLASS,
            onClick = { playlistVisible = true },
            onMenuClick = { playlistVisible = !playlistVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = responsiveLayout.contentSideInset + 16.dp, bottom = playlistBottomPadding)
                .zIndex(9f),
        )

        // Material 3 Expressive Buffering / Slow Network Snackbar
        // M3 spec: snackbar enters with spring(0.75f, 600f), exits with spring(0.85f, 800f)
        AnimatedVisibility(
            visible = state.isBuffering,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
                    stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
                ),
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = 1.0f,
                    stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
                ),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 800f),
            ) + fadeOut(
                animationSpec = spring(
                    dampingRatio = 1.0f,
                    stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = responsiveLayout.contentSideInset + 16.dp, bottom = playlistBottomPadding)
                .zIndex(10f),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2E3036), // M3 inverseSurface tone
                contentColor = Color(0xFFF0F0F5), // M3 inverseOnSurface tone
                shadowElevation = 6.dp,
                modifier = Modifier.height(48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = visualStyle.primaryContainer,
                    )
                    Text(
                        text = "Buffering stream\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF0F0F5),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // M3 Snackbar spec: optional action slot
                    Surface(
                        onClick = { /* dismiss handled by state change */ },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        contentColor = visualStyle.primaryContainer,
                    ) {
                        Text(
                            text = "Dismiss",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeBackdrop(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    background: Brush,
    coverPrimary: Color,
    coverSecondary: Color,
    alpha: Float,
) {
    when (visualTheme) {
        PlayerVisualTheme.FROSTED_GLASS -> {
            FrostedGlassBackdrop(
                state = state,
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
                alpha = alpha,
            )
        }
        PlayerVisualTheme.DUOTONE -> {
            DuotoneBackdrop(
                state = state,
                coverPrimary = coverPrimary,
                coverSecondary = coverSecondary,
                alpha = alpha,
            )
        }
        PlayerVisualTheme.HI_FI_STUDIO -> {
            // Keep the studio's dark chassis, but let the sleeve supply a quiet
            // ambient colour cast. This is deliberately a plain, cached gradient:
            // it follows the artwork without adding another expensive animated layer.
            val hifiBg = remember(coverPrimary, coverSecondary) {
                val topTint = coverPrimary
                    .scaledForBackground(0.34f)
                    .blendedForBackground(Color(0xFF171310), 0.46f)
                val middleTint = coverPrimary
                    .blendedForBackground(coverSecondary, 0.48f)
                    .scaledForBackground(0.28f)
                    .blendedForBackground(Color(0xFF110E0C), 0.54f)
                val bottomTint = coverSecondary
                    .scaledForBackground(0.22f)
                    .blendedForBackground(Color(0xFF080706), 0.70f)
                Brush.verticalGradient(
                    listOf(
                        topTint,
                        middleTint,
                        bottomTint,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
                    .background(hifiBg),
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
                    .background(background),
            )
        }
    }
}

@Composable
private fun FrostedGlassBackdrop(
    state: ExternalMediaState,
    coverPrimary: Color,
    coverSecondary: Color,
    alpha: Float,
) {
    // Convert the Bitmap once per artwork revision. Backdrop recompositions (clock, progress,
    // button press) should not repeatedly allocate a compose ImageBitmap.
    val artworkImage = remember(state.artwork, state.artworkSignature) { state.artwork?.asImageBitmap() }
    val fallback = remember(coverPrimary, coverSecondary) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF202D3A),
                Color(0xFF111922),
                Color(0xFF070B11),
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(fallback),
    ) {
        if (artworkImage != null) {
            Image(
                bitmap = artworkImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Artwork is a quiet refracted silhouette, never a full
                        // colour canvas (that visual role belongs to Duotone).
                        this.alpha = 0.18f
                        scaleX = 1.06f
                        scaleY = 1.06f
                    }
                    .frostedGlassLayeredBlur(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEAF6FF).copy(alpha = 0.13f),
                            Color(0xFFB9D9F4).copy(alpha = 0.045f),
                            Color.Transparent,
                        ),
                        center = Offset(0.16f, 0.06f),
                        radius = 1050f,
                    ),
                ),
        )
        // A small, cover-derived prism at the lower edge keeps the theme adaptive
        // without turning it into Duotone's broad, saturated artwork wash.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            coverPrimary.copy(alpha = 0.13f),
                            coverSecondary.copy(alpha = 0.045f),
                            Color.Transparent,
                        ),
                        center = Offset(0.82f, 0.92f),
                        radius = 760f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF071019).copy(alpha = 0.18f),
                            Color.Transparent,
                            Color(0xFF020407).copy(alpha = 0.58f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun DuotoneBackdrop(
    state: ExternalMediaState,
    coverPrimary: Color,
    coverSecondary: Color,
    alpha: Float,
) {
    // Spotify-style duotone canvas: full-bleed artwork crushed toward two hues
    // under a soft gradient wash, with a gentle drift so the backdrop feels alive.
    val ambientMotion = rememberAmbientMotionEnabled(state.isPlaying && !state.isBuffering)
    val artworkImage = remember(state.artwork, state.artworkSignature) { state.artwork?.asImageBitmap() }
    val infiniteTransition = rememberInfiniteTransition(label = "duotone drift")
    val rawPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (ambientMotion) (2f * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "duotone phase",
    )
    val driftX = kotlin.math.sin(rawPhase) * 22f
    val driftY = kotlin.math.cos(rawPhase * 0.7f) * 18f
    val scalePulse = 1.18f + kotlin.math.sin(rawPhase * 0.5f) * 0.05f

    val fallbackBg = remember(coverPrimary, coverSecondary) {
        Brush.radialGradient(
            colors = listOf(
                coverPrimary.copy(alpha = 0.9f),
                coverSecondary.copy(alpha = 0.7f),
                Color(0xFF0B0910),
            ),
            radius = 1400f,
        )
    }
    val wash = Brush.verticalGradient(
        listOf(
            coverPrimary.copy(alpha = 0.35f),
            Color.Transparent,
            coverSecondary.copy(alpha = 0.40f),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black),
    ) {
        if (artworkImage != null) {
            Image(
                bitmap = artworkImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scalePulse
                        scaleY = scalePulse
                        translationX = driftX
                        translationY = driftY
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                46f, 46f, android.graphics.Shader.TileMode.MIRROR
                            ).asComposeRenderEffect()
                        }
                    },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scalePulse
                        scaleY = scalePulse
                        translationX = driftX
                        translationY = driftY
                    }
                    .background(fallbackBg),
            )
        }
        // Duotone wash + vignette to keep the two-tone palette and control legibility.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(brush = wash)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                            radius = max(size.width, size.height) * 0.75f,
                        ),
                    )
                },
        )
    }
}

@Composable
private fun ThemePlayerLayout(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    style: PlayerVisualStyle,
    responsiveLayout: ResponsivePlayerLayout,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    if (!state.notificationAccessGranted || !state.connected) {
        AndroidExpressiveEmptyLayout(
            state = state,
            style = style,
            compact = responsiveLayout.compact,
            contentMaxWidth = responsiveLayout.contentMaxWidth,
            modifier = modifier,
        )
        return
    }
    if (responsiveLayout.wide) {
        ResponsiveWidePlayerLayout(
            state = state,
            visualTheme = visualTheme,
            style = style,
            responsiveLayout = responsiveLayout,
            modifier = modifier,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onSeek = onSeek,
            onVolumeChange = onVolumeChange,
            onLike = onLike,
            onShuffle = onShuffle,
        )
        return
    }
    when (visualTheme) {
        PlayerVisualTheme.FROSTED_GLASS -> {
            iOSGlassLayout(
                state = state,
                style = style,
                compact = responsiveLayout.compact,
                contentMaxWidth = responsiveLayout.contentMaxWidth,
                modifier = modifier,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onLike = onLike,
                onShuffle = onShuffle,
            )
        }
        PlayerVisualTheme.HI_FI_STUDIO -> {
            HiFiLayout(
                state = state,
                style = style,
                compact = responsiveLayout.compact,
                contentMaxWidth = responsiveLayout.contentMaxWidth,
                modifier = modifier,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onLike = onLike,
                onShuffle = onShuffle,
            )
        }
        PlayerVisualTheme.DUOTONE -> {
            DuotoneLayout(
                state = state,
                style = style,
                compact = responsiveLayout.compact,
                contentMaxWidth = responsiveLayout.contentMaxWidth,
                modifier = modifier,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onLike = onLike,
                onShuffle = onShuffle,
            )
        }
        else -> {
            AndroidExpressiveLayout(
                state = state,
                style = style,
                compact = responsiveLayout.compact,
                contentMaxWidth = responsiveLayout.contentMaxWidth,
                modifier = modifier,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onLike = onLike,
                onShuffle = onShuffle,
            )
        }
    }
}

@Composable
private fun ResponsiveWidePlayerLayout(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    style: PlayerVisualStyle,
    responsiveLayout: ResponsivePlayerLayout,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val isFrosted = visualTheme == PlayerVisualTheme.FROSTED_GLASS
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = responsiveLayout.contentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = if (responsiveLayout.compact) 16.dp else 24.dp),
        ) {
            if (responsiveLayout.showClock) {
                if (visualTheme == PlayerVisualTheme.MATERIAL_3_EXPRESSIVE) {
                    AndroidExpressiveClock(style = style, compact = true)
                } else {
                    CoverLockClock(
                        centered = false,
                        compact = true,
                        datePill = visualTheme == PlayerVisualTheme.FROSTED_GLASS,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // The art is governed by both available axes. This protects short landscape
                // phones from vertical clipping, while a tablet gets a satisfyingly large sleeve.
                val artworkSize = minOf(
                    maxWidth * 0.42f,
                    maxHeight * if (responsiveLayout.compact) 0.70f else 0.82f,
                    440.dp,
                ).coerceAtLeast(140.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(artworkSize),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (responsiveLayout.compact) 16.dp else 28.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkPanel(
                        artwork = state.artwork,
                        artworkSignature = state.artworkSignature,
                        style = style,
                        modifier = Modifier
                            .size(artworkSize)
                            .then(
                                if (isFrosted) {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.42f), style.artworkShape)
                                } else {
                                    Modifier
                                },
                            ),
                        elevation = if (responsiveLayout.compact) 10.dp else 18.dp,
                    )
                    ResponsiveWideControlPanel(
                        state = state,
                        visualTheme = visualTheme,
                        style = style,
                        compact = responsiveLayout.compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSeek = onSeek,
                        onVolumeChange = onVolumeChange,
                        onLike = onLike,
                        onShuffle = onShuffle,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveWideControlPanel(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    style: PlayerVisualStyle,
    compact: Boolean,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val isFrosted = visualTheme == PlayerVisualTheme.FROSTED_GLASS
    val liquidControls = visualTheme != PlayerVisualTheme.MATERIAL_3_EXPRESSIVE
    val panelShape = when (visualTheme) {
        PlayerVisualTheme.FROSTED_GLASS -> RoundedCornerShape(32.dp)
        PlayerVisualTheme.DUOTONE -> RoundedCornerShape(28.dp)
        else -> style.cardShape
    }
    val progressTreatment = when (visualTheme) {
        PlayerVisualTheme.FROSTED_GLASS -> ProgressTreatment.GLASS
        PlayerVisualTheme.DUOTONE, PlayerVisualTheme.HI_FI_STUDIO -> ProgressTreatment.EDITORIAL
        else -> ProgressTreatment.EXPRESSIVE
    }
    val panelContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 18.dp),
        ) {
            if (!compact && state.sourceApp.isNotBlank()) {
                SourceCapsule(
                    state = state,
                    style = style,
                    minimal = true,
                    uppercase = true,
                    showSignal = false,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = state.title.ifBlank { "Nothing Playing" },
                color = style.foreground,
                fontSize = if (compact) 19.sp else 27.sp,
                lineHeight = if (compact) 23.sp else 31.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist.ifBlank { state.sourceApp.ifBlank { "Media" } },
                color = style.secondaryForeground,
                fontSize = if (compact) 13.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
            ThemedPositionSection(
                state = state,
                style = style,
                onSeek = onSeek,
                treatment = progressTreatment,
            )
            Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResponsiveWideTransportControls(
                        state = state,
                        visualTheme = visualTheme,
                        style = style,
                        compact = true,
                        modifier = Modifier.weight(1f),
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onLike = onLike,
                        onShuffle = onShuffle,
                    )
                    VolumeControlRow(
                        state = state,
                        style = style,
                        onVolumeChange = onVolumeChange,
                        modifier = Modifier.weight(1f),
                        frostedGlass = isFrosted,
                    )
                }
            } else {
                ResponsiveWideTransportControls(
                    state = state,
                    visualTheme = visualTheme,
                    style = style,
                    compact = false,
                    modifier = Modifier.fillMaxWidth(),
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onLike = onLike,
                    onShuffle = onShuffle,
                )
                Spacer(Modifier.height(12.dp))
                VolumeControlRow(
                    state = state,
                    style = style,
                    onVolumeChange = onVolumeChange,
                    modifier = Modifier.fillMaxWidth(),
                    frostedGlass = isFrosted,
                )
            }
        }
    }
    if (isFrosted) {
        FrostedGlassPanel(
            style = style,
            shape = panelShape,
            modifier = modifier,
            compactHeight = compact,
            content = panelContent,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = panelShape,
            color = style.controlContainer.copy(alpha = if (visualTheme == PlayerVisualTheme.DUOTONE) 0.90f else 0.96f),
            contentColor = style.foreground,
            border = androidx.compose.foundation.BorderStroke(style.borderWidth, style.cardBorder),
            shadowElevation = if (compact) 6.dp else 12.dp,
            content = panelContent,
        )
    }
}

@Composable
private fun ResponsiveWideTransportControls(
    state: ExternalMediaState,
    visualTheme: PlayerVisualTheme,
    style: PlayerVisualStyle,
    compact: Boolean,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val liquidMotion = visualTheme != PlayerVisualTheme.MATERIAL_3_EXPRESSIVE
    val buttonSize = 48.dp // Keeps every control at Android's accessible minimum target.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Keep the short landscape row readable. Secondary actions remain available in the
        // portrait player and on larger wide panels, instead of squeezing primary transport.
        if (!compact && state.shuffleAction != null) {
            TransportButton(
                ResonanceIcons.Shuffle,
                "Shuffle",
                true,
                style,
                onShuffle,
                size = buttonSize,
                liquidMotion = liquidMotion,
            )
        }
        TransportButton(
            ResonanceIcons.SkipPrevious,
            "Previous",
            state.canGoPrevious,
            style,
            onPrevious,
            size = buttonSize,
            liquidMotion = liquidMotion,
        )
        when (visualTheme) {
            PlayerVisualTheme.FROSTED_GLASS -> FrostedHeroPlayButton(
                state = state,
                style = style,
                onClick = onPlayPause,
                size = if (compact) 52.dp else 64.dp,
            )
            PlayerVisualTheme.HI_FI_STUDIO -> ChromePlayButton(
                state = state,
                style = style,
                onClick = onPlayPause,
                size = if (compact) 52.dp else 64.dp,
            )
            PlayerVisualTheme.DUOTONE -> DuotonePlayButton(
                state = state,
                style = style,
                onClick = onPlayPause,
                size = if (compact) 52.dp else 64.dp,
            )
            else -> PlayPauseButton(
                state = state,
                style = style,
                onClick = onPlayPause,
                width = if (compact) 52.dp else 64.dp,
                expressiveMotion = true,
            )
        }
        TransportButton(
            ResonanceIcons.SkipNext,
            "Next",
            state.canGoNext,
            style,
            onNext,
            size = buttonSize,
            liquidMotion = liquidMotion,
        )
        if (!compact && state.canLike) {
            TransportButton(
                if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (state.isLiked) "Unlike" else "Like",
                true,
                style,
                onLike,
                size = buttonSize,
                contentColor = if (state.isLiked) style.primaryContainer else style.foreground,
                liquidMotion = liquidMotion,
            )
        }
    }
}

@Composable
private fun CoverLockClock(
    modifier: Modifier = Modifier,
    centered: Boolean = true,
    compact: Boolean = false,
    datePill: Boolean = false,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.US
    val timePattern = remember(context, locale) {
        if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    }
    val timeFormatter = remember(timePattern, locale) { SimpleDateFormat(timePattern, locale) }
    val dateFormatter = remember(locale) { SimpleDateFormat("EEE, d MMM", locale) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val delayToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L) + 40L
            delay(delayToNextMinute)
            now = System.currentTimeMillis()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = timeFormatter.format(Date(now)),
            color = Color.White,
            fontSize = if (compact) 50.sp else 58.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-2).sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        if (datePill) {
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = dateFormatter.format(Date(now)),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            Text(
                text = dateFormatter.format(Date(now)),
                color = Color.White.copy(alpha = 0.80f),
                style = MaterialTheme.typography.titleSmall,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun AndroidExpressiveClock(
    style: PlayerVisualStyle,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.US
    val timePattern = remember(context, locale) {
        if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    }
    val timeFormatter = remember(timePattern, locale) { SimpleDateFormat(timePattern, locale) }
    val weekdayFormatter = remember(locale) { SimpleDateFormat("EEEE", locale) }
    val dateFormatter = remember(locale) { SimpleDateFormat("d MMM", locale) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val delayToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L) + 40L
            delay(delayToNextMinute)
            now = System.currentTimeMillis()
        }
    }

    // Subtle cover-color tint blended into white for the clock display
    val clockColor = style.foreground.copy(
        red = style.foreground.red * 0.92f + style.primaryContainer.red * 0.08f,
        green = style.foreground.green * 0.92f + style.primaryContainer.green * 0.08f,
        blue = style.foreground.blue * 0.92f + style.primaryContainer.blue * 0.08f,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (compact) 10.dp else 14.dp),
    ) {
        Text(
            text = timeFormatter.format(Date(now)),
            color = clockColor,
            fontSize = if (compact) 52.sp else 64.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-3).sp,
            lineHeight = if (compact) 54.sp else 66.sp,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = weekdayFormatter.format(Date(now)),
                color = style.secondaryForeground.copy(alpha = 0.80f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "·",
                color = style.secondaryForeground.copy(alpha = 0.40f),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = dateFormatter.format(Date(now)),
                color = style.secondaryForeground.copy(alpha = 0.80f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun Color.scaledForBackground(factor: Float): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = 1f,
)

private fun Color.blendedForBackground(other: Color, otherFraction: Float): Color {
    val fraction = otherFraction.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = 1f,
    )
}

@Composable
private fun ArtworkPanel(
    artwork: Bitmap?,
    artworkSignature: Long,
    style: PlayerVisualStyle,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    elevation: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = style.artworkShape,
                ambientColor = style.shadowColor,
                spotColor = style.shadowColor,
            )
            .clip(style.artworkShape)
            .background(Color(0xFF090B10)),
            // Artwork intentionally has no outline; the cover edge should remain clean in every theme.
        contentAlignment = Alignment.Center,
    ) {
        StableArtworkImage(
            artwork = artwork,
            artworkSignature = artworkSignature,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PlaylistSplitButton(
    style: PlayerVisualStyle,
    expanded: Boolean,
    frostedGlass: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leadingInteraction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
    val leadingPressed by leadingInteraction.collectIsPressedAsState()
    val menuPressed by menuInteraction.collectIsPressedAsState()

    // M3 Expressive fast spatial spring specs via token references
    val leadingScale by animateFloatAsState(
        targetValue = if (leadingPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "playlist split leading scale",
    )
    val menuScale by animateFloatAsState(
        targetValue = if (menuPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "playlist split menu scale",
    )

    // M3 Split button inner corner morph via fast spatial spring
    val innerCorner by animateDpAsState(
        targetValue = if (expanded) 8.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "playlist split inner corner",
    )
    val menuRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "playlist split menu rotation",
    )
    val containerColor by animateColorAsState(
        targetValue = if (leadingPressed || menuPressed) {
            if (frostedGlass) Color(0xFF415B70).copy(alpha = 0.94f) else style.primaryContainer.copy(alpha = 0.88f)
        } else {
            if (frostedGlass) Color(0xFF263A4B).copy(alpha = 0.90f) else style.primaryContainer
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "playlist split color",
    )

    // M3 Expressive button height: 46dp with 23dp outer pill radius
    val leadingShape = RoundedCornerShape(
        topStart = 23.dp,
        topEnd = innerCorner,
        bottomEnd = innerCorner,
        bottomStart = 23.dp,
    )
    val trailingShape = RoundedCornerShape(
        topStart = innerCorner,
        topEnd = 23.dp,
        bottomEnd = 23.dp,
        bottomStart = innerCorner,
    )
    val contentColor = style.primaryContent

    Row(
        modifier = modifier
            .height(46.dp)
            .shadow(
                elevation = if (frostedGlass) 10.dp else 3.dp,
                shape = RoundedCornerShape(23.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SplitButtonSegment(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = leadingScale
                    scaleY = leadingScale
                },
            shape = leadingShape,
            color = containerColor,
            contentColor = contentColor,
            frostedGlass = frostedGlass,
            coverPrimary = style.coverPrimary,
            coverSecondary = style.coverSecondary,
            interactionSource = leadingInteraction,
            onClick = onClick,
            description = "Open playlist",
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        ) {
            Icon(
                ResonanceIcons.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "Playlist",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
        SplitButtonSegment(
            modifier = Modifier
                .width(44.dp)
                .graphicsLayer {
                    scaleX = menuScale
                    scaleY = menuScale
                },
            shape = trailingShape,
            color = containerColor,
            contentColor = contentColor,
            frostedGlass = frostedGlass,
            coverPrimary = style.coverPrimary,
            coverSecondary = style.coverSecondary,
            interactionSource = menuInteraction,
            onClick = onMenuClick,
            description = if (expanded) "Close playlist menu" else "Open playlist menu",
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = menuRotation },
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun SplitButtonSegment(
    modifier: Modifier,
    shape: RoundedCornerShape,
    color: Color,
    contentColor: Color,
    frostedGlass: Boolean,
    coverPrimary: Color,
    coverSecondary: Color,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    description: String,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: @Composable RowScope.() -> Unit,
) {
    // Do not use the layered blur on a foreground button: it blurs the label and
    // produces the washed-out, transparent playlist control seen on device.
    val glassSurface = if (frostedGlass) {
        Modifier
            .frostedGlassNoiseOverlay()
            .frostedGlassRimEnhanced(intensity = 1.10f)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = 1.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(color, shape)
            .then(glassSurface)
            .border(
                width = if (frostedGlass) 1.dp else 0.dp,
                color = if (frostedGlass) Color(0xFFD6E7F5).copy(alpha = 0.30f) else Color.Transparent,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun PlaylistOverlay(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    frostedGlass: Boolean,
    presentation: PlaylistPresentation,
    onDismiss: () -> Unit,
    onSelect: (PlaylistEntry) -> Unit,
    onOpenSourceApp: () -> Unit,
) {
    val sheetShape = style.cardShape
    val sheetColor = remember(style.controlContainer, frostedGlass) {
        if (frostedGlass) {
            // A readable, smoky glass base. Artwork colour is added as a light
            // bleed above this layer, rather than making the sheet see-through.
            Color(0xFF15171C).copy(alpha = 0.90f)
        } else if (style.controlContainer.alpha < 0.5f) {
            Color(0xFF1E1E28).copy(alpha = 0.94f)
        } else {
            style.controlContainer
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (frostedGlass) 0.30f else 0.45f))
            .clickable(onClick = onDismiss)
            .zIndex(8f),
        contentAlignment = if (presentation == PlaylistPresentation.SIDE_PANEL) {
            Alignment.CenterEnd
        } else {
            Alignment.BottomCenter
        },
    ) {
        AnimatedVisibility(
            visible = true,
            modifier = if (presentation == PlaylistPresentation.SIDE_PANEL) {
                Modifier
                    .fillMaxWidth(0.52f)
                    .widthIn(min = 320.dp, max = 480.dp)
                    .fillMaxHeight()
                    .padding(top = 28.dp, end = 18.dp, bottom = 28.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 78.dp)
            },
            enter = fadeIn(tween(120)) +
                expandVertically(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Bottom,
                ) + scaleIn(
                    initialScale = 0.86f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                ),
            exit = fadeOut(tween(90)) +
                shrinkVertically(
                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Bottom,
                ) + scaleOut(
                    targetScale = 0.86f,
                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (presentation == PlaylistPresentation.SIDE_PANEL) {
                            Modifier.fillMaxHeight()
                        } else {
                            Modifier
                        },
                    ),
                color = Color.Transparent,
                contentColor = style.foreground,
                shape = sheetShape,
                border = androidx.compose.foundation.BorderStroke(
                    if (frostedGlass) 1.dp else 1.dp,
                    if (frostedGlass) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.25f),
                ),
                shadowElevation = if (frostedGlass) 22.dp else 16.dp,
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(sheetShape)
                            .background(
                                if (frostedGlass) {
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF242831).copy(alpha = 0.94f),
                                            sheetColor,
                                            Color(0xFF101216).copy(alpha = 0.94f),
                                        ),
                                    )
                                } else {
                                    SolidColor(sheetColor)
                                },
                            )
                            .then(
                                if (frostedGlass) {
                                    Modifier
                                        .frostedGlassNoiseOverlay()
                                        .frostedGlassRimEnhanced(intensity = 1.10f)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .then(
                                if (presentation == PlaylistPresentation.SIDE_PANEL) {
                                    Modifier.fillMaxHeight()
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Playlist", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (state.playlist.isEmpty()) {
                                    "Queue unavailable"
                                } else {
                                    "${state.playlist.size} songs"
                                },
                                color = style.secondaryForeground,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close playlist",
                                tint = style.secondaryForeground,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.playlist.isEmpty()) {
                        Text(
                            text = "This player did not publish its queue. Open it and use Share to send songs or a playlist link to Resonance Material Control; imported items will appear here automatically.",
                            color = style.secondaryForeground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                        Button(
                            onClick = onOpenSourceApp,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = style.primaryContainer,
                                contentColor = style.primaryContent,
                            ),
                        ) {
                            Text("Open in ${state.sourceApp}")
                        }
                    } else {
                        LazyColumn(
                            modifier = if (presentation == PlaylistPresentation.SIDE_PANEL) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.heightIn(max = 300.dp)
                            },
                        ) {
                            items(state.playlist, key = { it.id }) { entry ->
                                PlaylistRow(
                                    entry = entry,
                                    style = style,
                                    frostedGlass = frostedGlass,
                                    onSelect = onSelect,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun PlaylistRow(
    entry: PlaylistEntry,
    style: PlayerVisualStyle,
    frostedGlass: Boolean,
    onSelect: (PlaylistEntry) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // M3 Expressive fast spatial spring for press feedback
    val rowScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "playlist row press",
    )
    val rowBgAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.12f else 0f,
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "playlist row bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (frostedGlass) Color(0xFF080A0E).copy(alpha = 0.58f + rowBgAlpha) else style.primaryContainer.copy(alpha = rowBgAlpha),
            )
            .border(
                width = if (frostedGlass) 0.5.dp else 0.dp,
                color = if (frostedGlass) Color.White.copy(alpha = 0.26f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onSelect(entry) },
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(style.primaryContainer.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ResonanceIcons.MusicNote, contentDescription = null, tint = style.foreground)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, color = style.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOf(entry.artist, entry.album).filter { it.isNotBlank() }.joinToString("  •  "),
                color = style.secondaryForeground,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StableArtworkImage(
    artwork: Bitmap?,
    artworkSignature: Long,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = artworkSignature,
        transitionSpec = {
            (fadeIn(tween(150)) + scaleIn(tween(180), initialScale = 0.985f)) togetherWith
                (fadeOut(tween(105)) + scaleOut(tween(125), targetScale = 1.01f))
        },
        label = "stable artwork",
    ) { signature ->
        // A player can republish an equivalent Bitmap on pause/play. Retaining one bitmap for
        // each sampled content signature prevents that callback from re-running the transition.
        val retainedArtwork = remember(signature) { artwork }
        if (retainedArtwork != null) {
            val imageBitmap = remember(retainedArtwork) { retainedArtwork.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = "Album artwork",
                contentScale = contentScale,
                modifier = modifier,
            )
        } else {
            Box(
                modifier = modifier.background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, Color(0xFF10131C)),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ResonanceIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}

@Composable
private fun AndroidExpressiveLayout(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    contentMaxWidth: Dp,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
        ) {
        AndroidExpressiveClock(style = style, compact = compact)
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        AndroidExpressiveHero(
            state = state,
            style = style,
            compact = compact,
            onPrevious = onPrevious,
            onNext = onNext,
            onLike = onLike,
            onShuffle = onShuffle,
        )
        Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
        // Source chip + metadata in open typography (no enclosing card)
        AndroidExpressiveMetadata(state = state, style = style, compact = compact)
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        // Progress scrubber with timestamps
        AndroidExpressiveProgress(
            state = state,
            style = style,
            onSeek = onSeek,
            compact = compact,
        )
        Spacer(Modifier.height(if (compact) 14.dp else 20.dp))
        // Grand 5-action transport controls row
        AndroidExpressiveTransportGroup(
            state = state,
            style = style,
            compact = compact,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onLike = onLike,
            onShuffle = onShuffle,
        )
        Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
        // Full-width dedicated volume control pill
        AndroidExpressiveVolume(
            state = state,
            style = style,
            onVolumeChange = onVolumeChange,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AndroidExpressiveHero(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    var horizontalDrag by remember(state.artworkSignature, state.title) { mutableFloatStateOf(0f) }
    val swipeThreshold = with(LocalDensity.current) { 68.dp.toPx() }
    val heroTranslation by animateFloatAsState(
        targetValue = horizontalDrag * 0.40f,
        animationSpec = spring(
            dampingRatio = 0.65f, // M3 Expressive fast spatial spring
            stiffness = 500f,
        ),
        label = "android expressive swipe translation",
    )
    val heroScale by animateFloatAsState(
        targetValue = if (horizontalDrag == 0f) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = 0.60f,
            stiffness = 800f,
        ),
        label = "android expressive swipe scale",
    )
    val playbackLabel = when {
        state.isBuffering -> "BUFFERING"
        state.isPlaying -> "PLAYING"
        else -> "PAUSED"
    }

    // Ambient glow beneath the artwork using cover primary color
    val glowAlpha by animateFloatAsState(
        targetValue = if (state.artwork != null) 0.38f else 0.18f,
        animationSpec = tween(durationMillis = 200),
        label = "artwork glow alpha",
    )
    val glowColor = style.primaryContainer.copy(alpha = glowAlpha)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 188.dp else 244.dp)
            .graphicsLayer {
                translationX = heroTranslation
                scaleX = heroScale
                scaleY = heroScale
                // M3 Expressive parallax tilt: subtle rotationY coupled to drag
                // offset. Max ±3° prevents the artwork from looking distorted.
                rotationY = (horizontalDrag / swipeThreshold * 3f).coerceIn(-3f, 3f)
                cameraDistance = 12f * density
            }
            .semantics {
                contentDescription = "Album artwork. Swipe left for next or right for previous."
                stateDescription = playbackLabel.lowercase()
            }
            .pointerInput(state.canGoPrevious, state.canGoNext, state.artworkSignature) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onDragCancel = { horizontalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        horizontalDrag += amount
                    },
                    onDragEnd = {
                        when {
                            horizontalDrag < -swipeThreshold && state.canGoNext -> onNext()
                            horizontalDrag > swipeThreshold && state.canGoPrevious -> onPrevious()
                        }
                        horizontalDrag = 0f
                    },
                )
            },
    ) {
        // Ambient glow layer behind artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 188.dp else 244.dp)
                .drawBehind {
                    drawCircle(
                        color = glowColor,
                        radius = size.width * 0.54f,
                        center = Offset(size.width / 2f, size.height * 0.70f),
                    )
                },
        )
        // Main artwork panel - no like/shuffle overlaid; moved to action row below
        ArtworkPanel(
            artwork = state.artwork,
            artworkSignature = state.artworkSignature,
            style = style,
            modifier = Modifier.fillMaxSize(),
            elevation = 22.dp,
        )
    }
}

@Composable
private fun AndroidExpressiveMetadata(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
) {
    // Source app chip — minimal pill, no heavy card
    val sourceColor = style.primaryContainer.copy(alpha = 0.20f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Live playback dot + source name chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Animated playback indicator dot
            val dotAlpha by animateFloatAsState(
                targetValue = if (state.isPlaying) 1f else 0.38f,
                animationSpec = tween(durationMillis = 200),
                label = "playback dot alpha",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(style.primaryContainer.copy(alpha = dotAlpha)),
            )
            Surface(
                color = sourceColor,
                shape = RoundedCornerShape(50),
                contentColor = style.secondaryForeground,
            ) {
                Text(
                    text = state.sourceApp.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = style.primaryContainer,
                )
            }
        }
        // Playback state badge
        val stateLabel = when {
            state.isBuffering -> "BUFFERING"
            state.isPlaying -> "NOW PLAYING"
            else -> "PAUSED"
        }
        Text(
            text = stateLabel,
            color = style.secondaryForeground.copy(alpha = 0.55f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
        )
    }
    Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
    // Title — prominent, full width, allows marquee on long titles
    AnimatedMetadataBlock(
        state = state,
        style = style,
        prominent = true,
        subduedMotion = true,
    )
}

@Composable
private fun AndroidExpressiveProgress(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onSeek: (Long) -> Unit,
    compact: Boolean,
) {
    // Flat, open scrubber — no enclosing surface card. Timestamps flank the slider.
    ThemedPositionSection(state, style, onSeek, ProgressTreatment.EXPRESSIVE)
}

@Composable
private fun ExpressiveMediaActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "$description action scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            style.primaryContainer
        } else if (pressed) {
            style.controlContainer.copy(alpha = 0.95f)
        } else {
            style.controlContainer.copy(alpha = 0.85f)
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "$description action container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) style.primaryContent else style.foreground,
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "$description action content",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "$description icon pop",
    )

    FilledIconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (selected) 4.dp else if (pressed) 0.dp else 2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Crossfade(
            targetState = icon,
            animationSpec = tween(durationMillis = 150),
            label = "$description icon crossfade",
        ) { currentIcon ->
            Icon(
                imageVector = currentIcon,
                contentDescription = description,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun AndroidExpressiveTransportGroup(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val hasSecondaryActions = state.canLike || state.shuffleAction != null
    val sideBtnSize = if (compact) 56.dp else 64.dp
    val sideIconSize = if (compact) 26.dp else 30.dp
    val actionBtnSize = if (compact) 46.dp else 52.dp
    val actionIconSize = if (compact) 20.dp else 24.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left flank: Shuffle
        if (state.shuffleAction != null) {
            ExpressiveMediaActionButton(
                icon = ResonanceIcons.Shuffle,
                description = "Shuffle",
                selected = false,
                style = style,
                onClick = onShuffle,
                size = actionBtnSize,
                iconSize = actionIconSize,
            )
        } else if (hasSecondaryActions) {
            // Symmetrical placeholder so Play/Pause remains perfectly centered
            Spacer(Modifier.size(actionBtnSize))
        }

        // Transport: Previous
        AndroidExpressiveTransportButton(
            icon = ResonanceIcons.SkipPrevious,
            description = "Previous",
            enabled = state.canGoPrevious,
            style = style,
            onClick = onPrevious,
            size = sideBtnSize,
            iconSize = sideIconSize,
        )

        // Transport: Play/Pause (Hero)
        AndroidExpressivePlayButton(
            state = state,
            style = style,
            onClick = onPlayPause,
            compact = compact,
        )

        // Transport: Next
        AndroidExpressiveTransportButton(
            icon = ResonanceIcons.SkipNext,
            description = "Next",
            enabled = state.canGoNext,
            style = style,
            onClick = onNext,
            size = sideBtnSize,
            iconSize = sideIconSize,
        )

        // Right flank: Like / Favorite
        if (state.canLike) {
            ExpressiveMediaActionButton(
                icon = if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                description = if (state.isLiked) "Unlike" else "Like",
                selected = state.isLiked,
                style = style,
                onClick = onLike,
                size = actionBtnSize,
                iconSize = actionIconSize,
            )
        } else if (hasSecondaryActions) {
            // Symmetrical placeholder so Play/Pause remains perfectly centered
            Spacer(Modifier.size(actionBtnSize))
        }
    }
}

@Composable
private fun AndroidExpressiveTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 30.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // M3 Expressive fast spatial spring (damping 0.6f, stiffness 800f)
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "$description expressive press",
    )
    // M3 pressed-state shadow: elevation drops to 0 on press, restores on release
    val pressedElevation by animateDpAsState(
        targetValue = if (pressed && enabled) 0.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "$description expressive elevation",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> style.secondaryContainer.copy(alpha = 0.38f)
            pressed -> style.secondaryContainer.copy(alpha = 0.85f)
            else -> style.secondaryContainer
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "$description expressive color",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> style.secondaryForeground.copy(alpha = 0.38f)
            else -> style.secondaryContent
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "$description expressive content color",
    )
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = pressedElevation,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.22f),
            ),
        shape = RoundedCornerShape(22.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
    }
}

@Composable
private fun AndroidExpressivePlayButton(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && state.canPlayPause) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "play expressive press",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (state.isPlaying) 24.dp else 38.dp,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "play expressive corner",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !state.canPlayPause -> style.primaryContainer.copy(alpha = 0.38f)
            pressed -> style.primaryContainer.copy(alpha = 0.88f)
            else -> style.primaryContainer
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "play expressive color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (state.canPlayPause) {
            style.primaryContent
        } else {
            style.secondaryForeground.copy(alpha = 0.38f)
        },
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "play expressive content color",
    )
    val playWidth = if (compact) 80.dp else 90.dp
    val playHeight = if (compact) 68.dp else 76.dp
    val playIconSize = if (compact) 32.dp else 38.dp

    val pressedElevation by animateDpAsState(
        targetValue = if (pressed && state.canPlayPause) 0.dp else 5.dp,
        animationSpec = spring(
            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
        ),
        label = "play elevation",
    )

    FilledIconButton(
        onClick = onClick,
        enabled = state.canPlayPause,
        interactionSource = interaction,
        modifier = modifier
            .size(width = playWidth, height = playHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = pressedElevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Crossfade(
            targetState = state.isPlaying,
            animationSpec = tween(
                durationMillis = ResonanceTokens.Motion.expressiveRippleDurationMs,
                easing = FastOutSlowInEasing,
            ),
            label = "android expressive play pause",
        ) { playing ->
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = contentColor,
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = if (playing) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(playIconSize),
                    tint = contentColor,
                )
            }
        }
    }
}

@Composable
private fun AndroidExpressiveVolume(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maximum = state.maxVolume.coerceAtLeast(1)
    val currentVolume = state.volume.coerceIn(0, maximum)
    val volumePercent = ((currentVolume * 100f) / maximum).roundToInt()

    // M3 Expressive: morph between three volume icon states
    val volumeIcon = when {
        currentVolume == 0 -> ResonanceIcons.VolumeOff
        currentVolume <= maximum / 3 -> ResonanceIcons.VolumeDown
        else -> ResonanceIcons.VolumeUp
    }

    // M3 fast effects spring for icon alpha tied to volume level
    val iconAlpha by animateFloatAsState(
        targetValue = (currentVolume.toFloat() / maximum.toFloat()).coerceIn(0.50f, 1f),
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
        ),
        label = "volume icon alpha",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .semantics {
                contentDescription = "Music volume"
                stateDescription = "$volumePercent percent"
            },
        color = style.controlContainer.copy(alpha = 0.88f),
        contentColor = style.foreground,
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, style.cardBorder),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // M3 Expressive: AnimatedContent crossfade between volume icon states
            AnimatedContent(
                targetState = volumeIcon,
                transitionSpec = {
                    (fadeIn(
                        animationSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
                        ),
                    ) + scaleIn(
                        initialScale = 0.80f,
                        animationSpec = spring(
                            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
                            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
                        ),
                    )) togetherWith (fadeOut(
                        animationSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = ResonanceTokens.Motion.fastEffectsStiffness,
                        ),
                    ) + scaleOut(
                        targetScale = 0.80f,
                        animationSpec = spring(
                            dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
                            stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
                        ),
                    ))
                },
                label = "volume icon morph",
            ) { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = style.primaryContainer.copy(alpha = iconAlpha),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            VolumeSlider(
                state = state,
                style = style,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            // Percentage badge pill
            Surface(
                color = style.primaryContainer.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
                contentColor = style.primaryContainer,
            ) {
                Text(
                    text = "$volumePercent%",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = style.primaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AndroidExpressiveEmptyLayout(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    contentMaxWidth: Dp,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(36.dp)
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
        ) {
        AndroidExpressiveClock(style = style, compact = compact)
        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 260.dp else 320.dp)
                .shadow(14.dp, shape, ambientColor = style.shadowColor, spotColor = style.shadowColor)
                .clip(shape)
                .background(style.cardBrush)
                .border(style.borderWidth, style.cardBorder, shape)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // M3 Expressive: breathing pulse animation for empty-state icon
                val breathingTransition = rememberInfiniteTransition(
                    label = "empty state breathing",
                )
                val breathingScale by breathingTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2000,
                            easing = FastOutSlowInEasing,
                        ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "empty icon breathe",
                )
                Surface(
                    color = style.primaryContainer,
                    contentColor = style.primaryContent,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    },
                ) {
                    Icon(
                        ResonanceIcons.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp).size(42.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (state.notificationAccessGranted) {
                        "Ready for your music"
                    } else {
                        "Media access needed"
                    },
                    color = style.foreground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = if (state.notificationAccessGranted) {
                        "Start playback in a supported music app. Your controls and artwork will appear here."
                    } else {
                        "Grant notification access inside Resonance Material Control so Android can securely expose media controls."
                    },
                    color = style.secondaryForeground,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = style.badgeContainer,
                    contentColor = style.badgeContent,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = if (state.notificationAccessGranted) "WAITING FOR PLAYBACK" else "SETUP REQUIRED",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun iOSGlassLayout(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    contentMaxWidth: Dp,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val trackKey = remember(state.title, state.artworkSignature) { "${state.title}|${state.artworkSignature}" }
    var artworkSettled by remember(trackKey) { mutableStateOf(false) }
    LaunchedEffect(trackKey) {
        artworkSettled = false
        delay(16L)
        artworkSettled = true
    }
    val artworkEnterScale by animateFloatAsState(
        targetValue = if (artworkSettled) 1f else 0.960f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 480f),
        label = "artwork enter scale",
    )
    val ambientMotion = rememberAmbientMotionEnabled(state.isPlaying && !state.isBuffering)
    val floatTransition = rememberInfiniteTransition(label = "glass float")
    val heroBob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = if (ambientMotion) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero bob",
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxWidth()
            .fillMaxHeight()
            // Sit the Frosted stack slightly lower on the lock screen without
            // changing the shared clock/status-bar positioning.
            .padding(top = if (compact) 12.dp else 18.dp)
            .padding(horizontal = if (compact) 16.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverLockClock(centered = true, compact = compact, datePill = true)
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
        Box(
            modifier = Modifier
                // Give Frosted Glass a stronger hero presence and use the tall
                // lock-screen canvas instead of leaving a large dead band.
                .fillMaxWidth(if (compact) 0.80f else 0.88f)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = artworkEnterScale
                    scaleY = artworkEnterScale
                    translationY = heroBob * 3f
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = style.artworkShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkPanel(
                artwork = state.artwork,
                artworkSignature = state.artworkSignature,
                style = style,
                modifier = Modifier.fillMaxSize(),
                elevation = 20.dp,
            )
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        // The metadata intentionally floats on the canvas; there is no separate
        // song-name tab in this version of Frosted Glass.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                state.title.ifBlank { "Nothing Playing" },
                color = style.foreground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                state.artist.ifBlank { state.sourceApp.ifBlank { "Media" } },
                color = style.secondaryForeground,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.sourceApp.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                // A compact source badge is readable here without covering the
                // album artwork or reintroducing the old full-width title tab.
                SourceCapsule(state, style, minimal = true, uppercase = true)
            }
        }
        // Keep playback connected to the current song instead of allowing the
        // tall lock-screen canvas to turn this into a large empty band.
        Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
        // One continuous playback dock: time/seek rail and transport controls now
        // share the same glass tab instead of reading as two unrelated surfaces.
        FrostedGlassPanel(style, RoundedCornerShape(36.dp), Modifier.fillMaxWidth(), showShadow = false) {
            Column(Modifier.fillMaxWidth()) {
                ThemedPositionSection(state, style, onSeek, ProgressTreatment.GLASS)
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    if (state.shuffleAction != null) TransportButton(ResonanceIcons.Shuffle, "Shuffle", true, style, onShuffle, size = 46.dp, liquidMotion = true)
                    TransportButton(ResonanceIcons.SkipPrevious, "Previous", state.canGoPrevious, style, onPrevious, size = if (compact) 50.dp else 54.dp, liquidMotion = true)
                    FrostedHeroPlayButton(state, style, onPlayPause, size = if (compact) 68.dp else 72.dp)
                    TransportButton(ResonanceIcons.SkipNext, "Next", state.canGoNext, style, onNext, size = if (compact) 50.dp else 54.dp, liquidMotion = true)
                    if (state.canLike) TransportButton(if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (state.isLiked) "Unlike" else "Like", true, style, onLike, size = 46.dp, contentColor = if (state.isLiked) style.primaryContainer else style.foreground, liquidMotion = true)
                }
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        // Keep the volume rail itself, but remove the extra enclosing tab.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            VolumeControlRow(state, style, onVolumeChange, capsule = false, frostedGlass = true, showThumb = true)
        }
        Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * A reusable floating frosted-glass panel: translucent fill, soft top specular
 * highlight, a hairline rim, and a light drop shadow. Used to separate the media
 * console into airy, individually-floating glass surfaces.
 */
@Composable
private fun FrostedGlassPanel(
    style: PlayerVisualStyle,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    compactHeight: Boolean = false,
    showShadow: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .then(
                if (showShadow) {
                    Modifier.shadow(
                        elevation = if (compactHeight) 10.dp else 14.dp,
                        shape = shape,
                        ambientColor = style.shadowColor,
                        spotColor = Color.White.copy(alpha = 0.12f),
                    )
                } else Modifier
            ),
        shape = shape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(style.borderWidth, style.cardBorder),
    ) {
        Box(
            modifier = Modifier,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(style.cardBrush)
                    .frostedGlassPanelEffects(
                        coverPrimary = style.coverPrimary,
                        coverSecondary = style.coverSecondary,
                    ),
            )
            Box(
                modifier = Modifier.padding(
                    horizontal = if (compactHeight) 14.dp else 18.dp,
                    vertical = if (compactHeight) 6.dp else 10.dp,
                ),
            ) {
                content()
            }
        }
    }
}

/**
 * The prominent glass play orb in the transport dock. Uses the shared
 * frostedGlassInteractive physics so it answers instantly on touch with a
 * luminance ripple and a snappy spring, matching the surrounding transport buttons.
 */
@Composable
private fun FrostedHeroPlayButton(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    size: Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val iconSize = if (size >= 66.dp) 30.dp else 26.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFD5E6F5).copy(alpha = 0.32f),
                        0.55f to Color(0xFF9EB8CD).copy(alpha = 0.14f),
                        1f to style.primaryContainer.copy(alpha = 0.12f),
                    ),
                    center = Offset(0.5f, 0.35f),
                    radius = 1f,
                )
            )
            .border(1.dp, Color(0xFFDCEBFA).copy(alpha = 0.40f), CircleShape)
            .frostedGlassInteractiveEnhanced(
                interactionSource = interaction,
                glowColor = Color.White,
                pressScale = 0.92f,
                enabled = state.canPlayPause,
                shape = CircleShape,
            )
            .clickable(
                enabled = state.canPlayPause,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = state.isPlaying,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            label = "frosted play pause",
        ) { playing ->
            if (state.isBuffering) {
                CircularProgressIndicator(
                    Modifier.size(if (size >= 66.dp) 26.dp else 24.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                )
            } else {
                Icon(
                    imageVector = if (playing) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(iconSize),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun HiFiLayout(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    contentMaxWidth: Dp,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val trackKey = remember(state.title, state.artworkSignature) { "${state.title}|${state.artworkSignature}" }
    var artworkSettled by remember(trackKey) { mutableStateOf(false) }
    LaunchedEffect(trackKey) {
        artworkSettled = false
        delay(16L)
        artworkSettled = true
    }
    val artworkEnterScale by animateFloatAsState(
        targetValue = if (artworkSettled) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 480f),
        label = "hifi artwork scale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Warm LED-style clock
        CoverLockClock(
            centered = true,
            compact = compact,
            datePill = false,
        )
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

        // ── Brushed-chrome vinyl deck ─────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = style.cardShape,
                    ambientColor = style.shadowColor,
                    spotColor = Color(0xFF3A3734),
                ),
            shape = style.cardShape,
            color = style.controlContainer,
            border = androidx.compose.foundation.BorderStroke(style.borderWidth, style.cardBorder),
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Vinyl artwork — square, tight corners like a record sleeve
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f)
                        .graphicsLayer {
                            scaleX = artworkEnterScale
                            scaleY = artworkEnterScale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ArtworkPanel(
                        artwork = state.artwork,
                        artworkSignature = state.artworkSignature,
                        style = style,
                        modifier = Modifier.fillMaxSize(),
                        elevation = 6.dp,
                    )
                }
                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                // Warm analog metadata readout
                Column(modifier = Modifier.fillMaxWidth()) {
                    // The title owns the full line; the source badge lives below it
                    // so a long song name cannot be covered or truncated early.
                    Text(
                        text = state.title.ifBlank { "Nothing Playing" },
                        color = style.foreground,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.artist.ifBlank { state.sourceApp.ifBlank { "Media" } },
                        color = style.secondaryForeground,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.sourceApp.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        SourceCapsule(state, style, minimal = true, uppercase = true)
                    }
                }

                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                // Analog VU meters as the progress / level readout
                HiFiVuMeter(state = state, style = style, compact = compact, onSeek = onSeek)

                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                // Chrome transport dials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.shuffleAction != null) {
                        TransportButton(
                            ResonanceIcons.Shuffle, "Shuffle", true, style, onShuffle,
                            size = 44.dp, liquidMotion = true,
                        )
                    }
                    TransportButton(
                        ResonanceIcons.SkipPrevious, "Previous", state.canGoPrevious, style, onPrevious,
                        size = 48.dp, liquidMotion = true,
                    )
                    ChromePlayButton(
                        state = state, style = style, onClick = onPlayPause,
                        size = if (compact) 60.dp else 66.dp,
                    )
                    TransportButton(
                        ResonanceIcons.SkipNext, "Next", state.canGoNext, style, onNext,
                        size = 48.dp, liquidMotion = true,
                    )
                    if (state.canLike) {
                        TransportButton(
                            icon = if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            description = if (state.isLiked) "Unlike" else "Like",
                            enabled = true, style = style, onClick = onLike, size = 44.dp,
                            contentColor = if (state.isLiked) Color(0xFFE8B873) else style.foreground,
                            liquidMotion = true,
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

                // Brushed volume rail
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = style.controlContainer,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, style.cardBorder),
                ) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                        VolumeControlRow(
                            state = state, style = style, onVolumeChange = onVolumeChange,
                            capsule = false, frostedGlass = true, showThumb = true,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ChromePlayButton(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    size: Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val iconSize = if (size >= 64.dp) 28.dp else 24.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFF5EFE6).copy(alpha = 0.7f),
                        0.5f to Color(0xFFC9892B).copy(alpha = 0.9f),
                        1f to Color(0xFF7A4A10).copy(alpha = 0.95f),
                    ),
                    center = Offset(0.5f, 0.3f),
                    radius = 1f,
                )
            )
            .border(1.dp, Color(0xFFE6E2DC).copy(alpha = 0.55f), CircleShape)
            .frostedGlassInteractiveEnhanced(
                interactionSource = interaction,
                glowColor = Color(0xFFC9892B),
                pressScale = 0.92f,
                enabled = state.canPlayPause,
                shape = CircleShape,
            )
            .clickable(
                enabled = state.canPlayPause,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = state.isPlaying,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            label = "chrome play pause",
        ) { playing ->
            if (state.isBuffering) {
                CircularProgressIndicator(
                    Modifier.size(if (size >= 64.dp) 24.dp else 22.dp),
                    strokeWidth = 3.dp,
                    color = style.primaryContent,
                )
            } else {
                Icon(
                    imageVector = if (playing) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(iconSize),
                    tint = style.primaryContent,
                )
            }
        }
    }
}

/** Analog amp-style equalizer used by the Hi-Fi theme as its progress / level readout. */
@Composable
private fun HiFiVuMeter(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    onSeek: (Long) -> Unit,
) {
    val maximum = state.durationMs.coerceAtLeast(1L)
    val playbackKey = remember(state.packageName, state.title, state.durationMs) {
        "${state.packageName}|${state.title}|${state.durationMs}"
    }
    var anchoredPosition by remember(playbackKey) {
        mutableFloatStateOf(state.positionMs.coerceIn(0L, maximum).toFloat())
    }
    var anchorElapsedMs by remember(playbackKey) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var frameElapsedMs by remember(playbackKey) { mutableLongStateOf(anchorElapsedMs) }
    var isDragging by remember(playbackKey) { mutableStateOf(false) }
    var dragFraction by remember(playbackKey) {
        mutableFloatStateOf((anchoredPosition / maximum.toFloat()).coerceIn(0f, 1f))
    }
    var seekHoldUntilMs by remember(playbackKey) { mutableLongStateOf(0L) }
    val playbackSpeed = state.playbackSpeed.takeIf { it.isFinite() }?.coerceIn(0f, 8f) ?: 0f
    val isAdvancing = state.isPlaying && !state.isBuffering && playbackSpeed > 0f

    // The VU bars are a seek control, so their lit length must follow the song monotonically.
    // Blend normal source updates into the local clock; only a true seek can move it abruptly.
    LaunchedEffect(playbackKey, state.positionMs, state.isPlaying, state.isBuffering, playbackSpeed) {
        if (!isDragging) {
            val now = SystemClock.elapsedRealtime()
            val projected = (anchoredPosition + if (isAdvancing) {
                (now - anchorElapsedMs).coerceAtLeast(0L) * playbackSpeed
            } else 0f).coerceIn(0f, maximum.toFloat())
            val reported = state.positionMs.coerceIn(0L, maximum).toFloat()
            val correction = reported - projected
            anchoredPosition = when {
                now < seekHoldUntilMs && abs(correction) > 1_200f -> projected
                abs(correction) > 1_200f -> reported
                else -> (projected + correction * 0.16f).coerceIn(0f, maximum.toFloat())
            }
            anchorElapsedMs = now
            frameElapsedMs = now
        }
    }
    LaunchedEffect(playbackKey, isAdvancing, playbackSpeed) {
        while (isAdvancing) {
            frameElapsedMs = SystemClock.elapsedRealtime()
            delay(50L)
        }
    }
    val smoothPosition = (anchoredPosition + if (isAdvancing) {
        (frameElapsedMs - anchorElapsedMs).coerceAtLeast(0L) * playbackSpeed
    } else 0f).coerceIn(0f, maximum.toFloat())
    val fraction by animateFloatAsState(
        targetValue = (smoothPosition / maximum.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "hifi song position",
    )
    LaunchedEffect(fraction, isDragging) { if (!isDragging) dragFraction = fraction }

    // Amp envelope: a phase that only advances while playing, so the bars crawl
    // forward as the song plays. Each band is derived from this shared phase with
    // its own offset + speed so highs (left) and lows (right) dance independently.
    val ambientMotion = rememberAmbientMotionEnabled(state.isPlaying && !state.isBuffering)
    val ampTransition = rememberInfiniteTransition(label = "hifi amp")
    val ampPhase by ampTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (ambientMotion) (2f * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hifi amp phase",
    )

    val bandCount = if (compact) 14 else 20
    // Slimmer bars toward the highs (left), taller toward the lows (right).
    val barHeight = if (compact) 40.dp else 48.dp

    Box(
        modifier = Modifier.fillMaxWidth().height(barHeight),
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val playing = state.isPlaying
            val progress = (if (isDragging) dragFraction else fraction).coerceIn(0f, 1f)
            val rightEdge = size.width * progress
            val gap = 2.dp.toPx()
            val slotW = size.width / bandCount.toFloat()
            val barW = (slotW - gap).coerceAtLeast(1f)
            val phase = if (playing) ampPhase else 0f

            repeat(bandCount) { i ->
                // i = 0 → high freq (left), i = bandCount-1 → low freq (right).
                // Normalized 0..1 position across the rail, left to right.
                val t = i / (bandCount - 1f)
                val x = i * slotW
                val barRight = x + barW
                // Only bands under the played segment are "active"; the rest stay dim
                // so the song-length is still readable from the lit span.
                val playedSpan = rightEdge >= barRight
                // Pseudo-spectrum built from INTEGER harmonics of the shared phase, so
                // the equalizer is exactly periodic in 2π and wraps seamlessly (uniform
                // flow). Each band only varies via a phase offset at that spatial point,
                // plus an envelope that shelves highs down and lows up.
                val off = t * 2.4f
                val raw = 0.5f +
                    0.24f * kotlin.math.sin(phase + off) +
                    0.15f * kotlin.math.sin(2f * phase + 2f * off) +
                    0.08f * kotlin.math.sin(3f * phase + 3f * off) +
                    0.045f * kotlin.math.sin(4f * phase + 4f * off)
                // Lows have more body; highs taper off quickly like a real EQ shelf.
                val band = (if (playing) raw else 0.16f) * (0.40f + t * 0.60f)
                val h = (size.height * band.coerceIn(0.04f, 1f)).coerceAtLeast(2f)

                val barTop = size.height - h
                val c = if (!playedSpan) {
                    style.sliderInactive
                } else if (h > size.height * 0.82f) {
                    // Clipping red at the top, like an analog peak.
                    Color(0xFFE8421B)
                } else {
                    style.primaryContainer
                }
                drawRoundRect(
                    color = c,
                    topLeft = androidx.compose.ui.geometry.Offset(x, barTop),
                    size = androidx.compose.ui.geometry.Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f),
                )
            }

            // Thin progress rail baseline so the played span reads as a measurement.
            drawLine(
                color = style.secondaryForeground.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1f),
                strokeWidth = 1f,
            )
        }
        // Invisible seek layer
        Slider(
            value = (if (isDragging) dragFraction else fraction) * maximum.toFloat(),
            onValueChange = { isDragging = true; dragFraction = (it / maximum.toFloat()).coerceIn(0f, 1f) },
            onValueChangeFinished = {
                val now = SystemClock.elapsedRealtime()
                anchoredPosition = (dragFraction * maximum.toFloat()).coerceIn(0f, maximum.toFloat())
                anchorElapsedMs = now
                frameElapsedMs = now
                seekHoldUntilMs = now + 750L
                onSeek(anchoredPosition.toLong())
                isDragging = false
            },
            valueRange = 0f..maximum.toFloat(),
            modifier = Modifier.matchParentSize(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

/** Spotify-style duotone editorial canvas: bold artwork + type + rounded pills. */
@Composable
private fun DuotoneLayout(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    compact: Boolean,
    contentMaxWidth: Dp,
    modifier: Modifier,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
) {
    val trackKey = remember(state.title, state.artworkSignature) { "${state.title}|${state.artworkSignature}" }
    var artworkSettled by remember(trackKey) { mutableStateOf(false) }
    LaunchedEffect(trackKey) {
        artworkSettled = false
        delay(16L)
        artworkSettled = true
    }
    val artworkEnterScale by animateFloatAsState(
        targetValue = if (artworkSettled) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f),
        label = "duotone artwork scale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Editorial clock over the duotone wash
        CoverLockClock(centered = true, compact = compact, datePill = false)
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        // Full-bleed duotone hero artwork
        Box(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.86f else 0.92f)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = artworkEnterScale
                    scaleY = artworkEnterScale
                }
                .clip(style.artworkShape)
                .border(1.dp, style.cardBorder, style.artworkShape),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkPanel(
                artwork = state.artwork,
                artworkSignature = state.artworkSignature,
                style = style,
                modifier = Modifier.fillMaxSize(),
                elevation = 4.dp,
            )
        }

        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

        // Bold editorial metadata centered
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.title.ifBlank { "Nothing Playing" },
                color = style.foreground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.artist.ifBlank { state.sourceApp.ifBlank { "Media" } },
                color = style.secondaryForeground,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        // Gradient wash progress
        ThemedPositionSection(
            state = state, style = style, onSeek = onSeek,
            treatment = ProgressTreatment.EDITORIAL,
        )

        Spacer(Modifier.weight(1f))

        // Rounded transport pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.shuffleAction != null) {
                TransportButton(
                    ResonanceIcons.Shuffle, "Shuffle", true, style, onShuffle,
                    size = 46.dp, liquidMotion = true,
                )
            }
            TransportButton(
                ResonanceIcons.SkipPrevious, "Previous", state.canGoPrevious, style, onPrevious,
                size = 48.dp, liquidMotion = true,
            )
            DuotonePlayButton(
                state = state, style = style, onClick = onPlayPause,
                size = if (compact) 64.dp else 70.dp,
            )
            TransportButton(
                ResonanceIcons.SkipNext, "Next", state.canGoNext, style, onNext,
                size = 48.dp, liquidMotion = true,
            )
            if (state.canLike) {
                TransportButton(
                    icon = if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    description = if (state.isLiked) "Unlike" else "Like",
                    enabled = true, style = style, onClick = onLike, size = 46.dp,
                    contentColor = if (state.isLiked) style.primaryContainer else style.foreground,
                    liquidMotion = true,
                )
            }
        }

        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

        // Duotone gradient volume wash
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = style.controlContainer,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, style.cardBorder),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                VolumeControlRow(
                    state = state, style = style, onVolumeChange = onVolumeChange,
                    capsule = false, frostedGlass = true, showThumb = true,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DuotonePlayButton(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    size: Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = style.shadowColor,
                spotColor = Color.White.copy(alpha = 0.3f),
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.3f),
                        0.55f to style.primaryContainer.copy(alpha = 0.9f),
                        1f to style.primaryContainer,
                    ),
                    center = Offset(0.5f, 0.35f),
                    radius = 1f,
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .frostedGlassInteractiveEnhanced(
                interactionSource = interaction,
                glowColor = Color.White,
                pressScale = 0.92f,
                enabled = state.canPlayPause,
                shape = CircleShape,
            )
            .clickable(
                enabled = state.canPlayPause,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = state.isPlaying,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            label = "duotone play pause",
        ) { playing ->
            if (state.isBuffering) {
                CircularProgressIndicator(
                    Modifier.size(if (size >= 68.dp) 26.dp else 24.dp),
                    strokeWidth = 3.dp,
                    color = style.primaryContent,
                )
            } else {
                Icon(
                    imageVector = if (playing) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(if (size >= 68.dp) 30.dp else 26.dp),
                    tint = style.primaryContent,
                )
            }
        }
    }
}

private data class TrackText(
    val title: String,
    val supporting: String,
)

@Composable
private fun AnimatedMetadataBlock(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    centered: Boolean = false,
    prominent: Boolean = false,
    subduedMotion: Boolean = false,
    frostedGlass: Boolean = false,
) {
    val supporting = remember(state.artist, state.album) {
        listOf(state.artist, state.album)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
    }
    val track = remember(state.title, supporting) { TrackText(state.title, supporting) }
    AnimatedContent(
        targetState = track,
        transitionSpec = {
            (fadeIn(tween(120)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = if (subduedMotion) 900f else 700f,
                ),
            ) { if (subduedMotion) it / 8 else it / 3 }) togetherWith
                (fadeOut(tween(90)) + slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 1400f,
                    ),
                ) { if (subduedMotion) -it / 10 else -it / 5 })
        },
        label = "track metadata",
    ) { visibleTrack ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                visibleTrack.title,
                color = style.foreground,
                style = if (prominent) {
                    MaterialTheme.typography.headlineSmall
                } else if (frostedGlass) {
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.2).sp,
                    )
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = when {
                    prominent -> FontWeight.ExtraBold
                    frostedGlass -> FontWeight.SemiBold
                    else -> FontWeight.Bold
                },
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                visibleTrack.supporting,
                color = style.secondaryForeground,
                style = if (frostedGlass) {
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                } else {
                    LocalTextStyle.current
                },
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditorialMetadataBlock(state: ExternalMediaState, style: PlayerVisualStyle) {
    AnimatedContent(
        targetState = TrackText(state.title, state.artist),
        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
        label = "editorial metadata",
    ) { track ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                track.title,
                color = style.foreground,
                fontSize = 26.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                track.supporting,
                color = style.secondaryForeground,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class ProgressTreatment {
    GLASS,
    EXPRESSIVE,
    EDITORIAL,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThemedPositionSection(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onSeek: (Long) -> Unit,
    treatment: ProgressTreatment,
) {
    val maximum = state.durationMs.coerceAtLeast(1L)
    var livePosition by remember(state.packageName, state.title, state.durationMs) {
        mutableLongStateOf(state.positionMs.coerceIn(0L, maximum))
    }
    LaunchedEffect(state.positionMs, state.playbackSpeed, state.isPlaybackActive, maximum) {
        livePosition = state.positionMs.coerceIn(0L, maximum)
        val speed = state.playbackSpeed.takeIf { it.isFinite() } ?: 0f
        while (state.isPlaybackActive && speed != 0f) {
            delay(250L)
            livePosition = (livePosition + (250L * speed).toLong()).coerceIn(0L, maximum)
        }
    }
    val animatedPosition by animateFloatAsState(
        targetValue = livePosition.toFloat(),
        animationSpec = when (treatment) {
            // M3 Expressive's wavy progress indicator uses a 500 ms linear progress
            // transition so the playhead never overshoots the actual media position.
            ProgressTreatment.EXPRESSIVE -> tween(
                durationMillis = 500,
                easing = LinearEasing,
            )
            ProgressTreatment.GLASS -> ResonanceTokens.Motion.liquidInteractive
            else -> spring(dampingRatio = 0.6f, stiffness = 700f)
        },
        label = "themed playback progress",
    )
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(animatedPosition) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(animatedPosition, isDragging) {
        if (!isDragging) dragPosition = animatedPosition
    }
    val displayedPosition = if (isDragging) dragPosition else animatedPosition
    val fraction = (displayedPosition / maximum.toFloat()).coerceIn(0f, 1f)
    val expressiveWaveTarget = if (
        treatment == ProgressTreatment.EXPRESSIVE &&
        fraction > 0.10f &&
        fraction < 0.95f
    ) {
        1f
    } else {
        0f
    }
    val expressiveWaveReveal by animateFloatAsState(
        targetValue = expressiveWaveTarget,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing,
        ),
        label = "expressive wave amplitude",
    )
    val dragEnergy by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = if (treatment == ProgressTreatment.GLASS) {
            ResonanceTokens.Motion.liquidInteractive
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 1400f,
            )
        },
        label = "waveform drag energy",
    )
    val progressInteraction = remember { MutableInteractionSource() }
    val ambientMotion = rememberAmbientMotionEnabled(state.isPlaying && !state.isBuffering)
    val waveformTransition = rememberInfiniteTransition(label = "neon waveform motion")
    val waveformPhase by waveformTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (ambientMotion) (Math.PI * 2.0).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (treatment == ProgressTreatment.EXPRESSIVE) 1000 else 1150,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "neon waveform phase",
    )
    val railHeight = when (treatment) {
        ProgressTreatment.EXPRESSIVE -> 56.dp
        else -> 28.dp
    }

    Column {
        Box(Modifier.fillMaxWidth().height(railHeight)) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val centerY = size.height / 2f
                val activeX = size.width * fraction
                // Material 3 Expressive's determinate wavy indicator is flat for
                // the first 10% and last 5% of progress, then uses its full wave.
                // This keeps the beginning visibly straight while the played
                // portion becomes the animated zig-zag.
                val waveReveal = if (treatment == ProgressTreatment.EXPRESSIVE) {
                    expressiveWaveReveal
                } else if (treatment == ProgressTreatment.GLASS) {
                    // Liquid Glass uses Apple's quiet capsule rail; the
                    // expressive wave belongs only to the M3 treatment.
                    0f
                } else {
                    ((fraction - 0.01f) / 0.24f).coerceIn(0f, 1f)
                }
                val amplitude = when (treatment) {
                    ProgressTreatment.GLASS -> 0f
                    // M3 Expressive linear wavy indicator token.
                    ProgressTreatment.EXPRESSIVE -> 4.dp.toPx()
                    ProgressTreatment.EDITORIAL -> 2.5.dp.toPx()
                } * waveReveal * (1f + dragEnergy * 0.35f) * (if (state.isPlaying) 1f else 0f)
                val thickness = when (treatment) {
                    ProgressTreatment.GLASS -> 2.5.dp.toPx()
                    // M3 Expressive linear indicator active and track thickness.
                    ProgressTreatment.EXPRESSIVE -> 5.dp.toPx()
                    ProgressTreatment.EDITORIAL -> 2.dp.toPx()
                }
                val phase = if (state.isPlaying) waveformPhase else 0f
                val cycles = when (treatment) {
                    ProgressTreatment.GLASS -> 4.5f
                    ProgressTreatment.EXPRESSIVE -> 5f
                    ProgressTreatment.EDITORIAL -> 4f
                }
                val expressiveWavelengthPx = 40.dp.toPx()
                fun snakeY(x: Float): Float {
                    val angle = if (treatment == ProgressTreatment.EXPRESSIVE) {
                        (x / expressiveWavelengthPx.coerceAtLeast(1f)) *
                            (Math.PI * 2.0).toFloat() + phase
                    } else {
                        (x / size.width.coerceAtLeast(1f)) *
                            (Math.PI * cycles).toFloat() + phase
                    }
                    return centerY + sin(angle) * amplitude
                }
                fun snakePath(endX: Float): Path {
                    val path = Path()
                    val clampedEnd = endX.coerceIn(0f, size.width)
                    val step = (size.width / 72f).coerceAtLeast(3f)
                    path.moveTo(0f, snakeY(0f))
                    var x = step
                    while (x < clampedEnd) {
                        path.lineTo(x, snakeY(x))
                        x += step
                    }
                    path.lineTo(clampedEnd, snakeY(clampedEnd))
                    return path
                }
                val trackGap = if (treatment == ProgressTreatment.EXPRESSIVE) 4.dp.toPx() else 0f
                drawLine(
                    color = style.sliderInactive,
                    start = androidx.compose.ui.geometry.Offset(
                        (activeX + trackGap).coerceAtMost(size.width),
                        centerY,
                    ),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = thickness,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                if (activeX > 0f) {
                    drawPath(
                        path = snakePath(activeX),
                        color = style.primaryContainer,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = if (treatment == ProgressTreatment.EXPRESSIVE) {
                                thickness
                            } else {
                                thickness * 1.12f
                            },
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                }
                // ── Vertical capsule thumb ────────────────────────────────────────────
                // Sits fixed on the center rail (not riding the wave) so it always reads
                // clearly as the scrub handle regardless of waveform amplitude.
                val capsuleW = (if (isDragging) 5.dp else 4.dp).toPx()
                val capsuleH = (if (isDragging) 22.dp else 18.dp).toPx()
                val capsuleX = activeX.coerceIn(capsuleW / 2f, size.width - capsuleW / 2f)
                drawRoundRect(
                    color = style.primaryContainer,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        capsuleX - capsuleW / 2f,
                        centerY - capsuleH / 2f,
                    ),
                    size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2f),
                )
            }
            Slider(
                value = displayedPosition,
                onValueChange = {
                    if (!isDragging) {
                        // M3 Expressive: haptic tick on drag start
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    isDragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    livePosition = dragPosition.toLong()
                    onSeek(livePosition)
                    isDragging = false
                    // M3 Expressive: haptic confirm on seek commit
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                enabled = state.canSeek,
                valueRange = 0f..maximum.toFloat(),
                modifier = Modifier.matchParentSize(),
                interactionSource = progressInteraction,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                ),
                // Keep the platform Slider's transparent thumb/track slots. Its
                // native gesture and semantics layer remains intact while the
                // Canvas above supplies the themed visual treatment.
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                formatPlaybackTime(displayedPosition.toLong()),
                color = style.secondaryForeground,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatPlaybackTime(state.durationMs),
                color = style.secondaryForeground,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun BareTransportControls(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    playWidth: Dp,
    sideSize: Dp = playWidth,
    sideContainer: Color = Color.Transparent,
    liquidMotion: Boolean = false,
    expressiveMotion: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TransportButton(
                ResonanceIcons.SkipPrevious, "Previous", state.canGoPrevious, style, onPrevious,
                size = sideSize,
                containerColor = if (liquidMotion) sideContainer else style.secondaryContainer,
                contentColor = if (liquidMotion) style.foreground else style.secondaryContent,
                liquidMotion = liquidMotion,
                expressiveMotion = expressiveMotion,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PlayPauseButton(
                state,
                style,
                onPlayPause,
                playWidth,
                liquidMotion = liquidMotion,
                expressiveMotion = expressiveMotion,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TransportButton(
                ResonanceIcons.SkipNext, "Next", state.canGoNext, style, onNext,
                size = sideSize,
                containerColor = if (liquidMotion) sideContainer else style.secondaryContainer,
                contentColor = if (liquidMotion) style.foreground else style.secondaryContent,
                liquidMotion = liquidMotion,
                expressiveMotion = expressiveMotion,
            )
        }
    }
}

@Composable
private fun LedVolumeControl(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onVolumeChange: (Int) -> Unit,
) {
    val maximum = state.maxVolume.coerceAtLeast(1)
    var isDragging by remember { mutableStateOf(false) }
    var dragVolume by remember(state.volume) { mutableFloatStateOf(state.volume.toFloat()) }
    val animatedVolume by animateFloatAsState(
        targetValue = state.volume.toFloat(),
        animationSpec = ResonanceTokens.Motion.fastSpatial,
        label = "neon volume",
    )
    val displayed = if (isDragging) dragVolume else animatedVolume
    val fraction = (displayed / maximum).coerceIn(0f, 1f)
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = ResonanceTokens.Motion.fastSpatial,
        label = "neon volume interaction",
    )
    Row(
        modifier = Modifier.graphicsLayer {
            scaleX = dragScale
            scaleY = dragScale
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            ResonanceIcons.VolumeUp,
            contentDescription = "Music volume",
            tint = style.secondaryForeground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(34.dp)) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val segments = 12
                val gap = 4.dp.toPx()
                val width = ((size.width - gap * (segments - 1)) / segments).coerceAtLeast(1f)
                repeat(segments) { index ->
                    val x = index * (width + gap)
                    drawRoundRect(
                        color = if ((index + 1f) / segments <= fraction) {
                            style.primaryContainer
                        } else {
                            style.sliderInactive
                        },
                        topLeft = androidx.compose.ui.geometry.Offset(x, 9.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(width, 16.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                }
            }
            Slider(
                value = displayed,
                onValueChange = { isDragging = true; dragVolume = it },
                onValueChangeFinished = {
                    onVolumeChange(dragVolume.roundToInt())
                    isDragging = false
                },
                valueRange = 0f..maximum.toFloat(),
                modifier = Modifier.matchParentSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SourceCapsule(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    minimal: Boolean = false,
    uppercase: Boolean = false,
    showSignal: Boolean = true,
) {
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.US
    val sourceLabel = remember(state.sourceApp, uppercase, locale) {
        if (uppercase) state.sourceApp.uppercase(locale) else state.sourceApp
    }
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = modifier.then(
            if (outlined) Modifier.border(1.dp, style.cardBorder, shape) else Modifier,
        ),
        color = if (minimal) Color.Transparent else style.badgeContainer,
        contentColor = style.badgeContent,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (minimal) 0.dp else 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ResonanceIcons.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                sourceLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (showSignal) {
                Spacer(Modifier.width(9.dp))
                PlaybackSignal(isPlaying = state.isPlaying, color = style.badgeContent)
            }
        }
    }
}

@Composable
private fun PlaybackSignal(isPlaying: Boolean, color: Color) {
    // M3 Expressive: staggered looping bounce when playing, M3 fast spatial
    // spring settle to static bars when paused.
    val ambientMotion = rememberAmbientMotionEnabled(isPlaying)
    val transition = rememberInfiniteTransition(label = "signal bars")
    val barConfigs = remember { listOf(
        Triple(9.dp, 5.dp, 0),      // bar 0: height, min, delay
        Triple(13.dp, 4.dp, 120),    // bar 1
        Triple(7.dp, 6.dp, 240),     // bar 2
    ) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        barConfigs.forEachIndexed { index, (playingHeight, minHeight, delayMs) ->
            // Continuous looping phase when playing
            val loopPhase by transition.animateFloat(
                initialValue = 0f,
                targetValue = if (ambientMotion) 1f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = delayMs,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "signal bar $index phase",
            )
            // M3 fast spatial spring for play/pause state transition
            val playingFactor by animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = ResonanceTokens.Motion.fastSpatialDamping,
                    stiffness = ResonanceTokens.Motion.fastSpatialStiffness,
                ),
                label = "signal bar $index state",
            )
            // When playing: bars oscillate between minHeight and playingHeight.
            // When paused: all bars settle to 4dp via spring.
            val animatedHeight = if (playingFactor > 0.01f) {
                val range = playingHeight - minHeight
                minHeight + range * loopPhase * playingFactor
            } else {
                4.dp
            }

            Box(
                Modifier
                    .width(2.5.dp)
                    .height(animatedHeight)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.82f)),
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    playWidth: Dp,
    sideSize: Dp = 54.dp,
    grouped: Boolean = false,
    liquidMotion: Boolean = false,
    expressiveMotion: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TransportButton(
                    ResonanceIcons.SkipPrevious,
                    "Previous",
                    state.canGoPrevious,
                    style,
                    onPrevious,
                    size = sideSize,
                    containerColor = if (grouped) Color.Transparent else style.controlContainer,
                    liquidMotion = liquidMotion,
                    expressiveMotion = true,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PlayPauseButton(
                    state,
                    style,
                    onPlayPause,
                    playWidth,
                    liquidMotion = liquidMotion,
                    expressiveMotion = true,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TransportButton(
                    ResonanceIcons.SkipNext,
                    "Next",
                    state.canGoNext,
                    style,
                    onNext,
                    size = sideSize,
                    containerColor = if (grouped) Color.Transparent else style.controlContainer,
                    liquidMotion = liquidMotion,
                    expressiveMotion = true,
                )
            }
        }
    }
    if (grouped) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = style.controlContainer,
            contentColor = style.foreground,
            shape = RoundedCornerShape(34.dp),
        ) {
            Box(Modifier.padding(5.dp)) { content() }
        }
    } else {
        content()
    }
}

@Composable
private fun PlayPauseButton(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    width: Dp,
    liquidMotion: Boolean = false,
    expressiveMotion: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (state.isPlaying) 18.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 800f,
        ),
        label = "play expressive corner",
    )
    val buttonShape = if (liquidMotion) CircleShape else RoundedCornerShape(cornerRadius)
    val playContentColor = if (liquidMotion) style.foreground else style.primaryContent
    val scale by animateFloatAsState(
        targetValue = if (pressed && state.canPlayPause) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 900f,
        ),
        label = "play press scale",
    )
    Box(
        modifier = Modifier
            .size(width)
            .shadow(
                elevation = if (liquidMotion) 0.dp else 1.dp,
                shape = buttonShape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            )
            .clip(buttonShape)
            .background(
                if (liquidMotion) {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to style.primaryContainer.copy(alpha = 0.34f),
                            0.58f to Color.White.copy(alpha = 0.16f),
                            1f to style.controlContainer.copy(alpha = 0.12f),
                        ),
                    )
                } else {
                    SolidColor(style.primaryContainer)
                },
                buttonShape,
            )
            .then(
                if (liquidMotion) {
                    Modifier
                        // Inner controls are translucent overlays on one glass
                        // plane; the lens rim belongs to the outer panel.
                        .border(1.dp, Color.White.copy(alpha = 0.10f), buttonShape)
                        .frostedGlassInteractiveEnhanced(
                            interactionSource = interaction,
                            glowColor = style.primaryContainer,
                            shape = buttonShape,
                        )
                } else {
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                }
            )
            .clickable(
                enabled = state.canPlayPause,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = state.isPlaying,
            animationSpec = tween(
                durationMillis = if (expressiveMotion) {
                    ResonanceTokens.Motion.expressiveRippleDurationMs
                } else {
                    90
                },
            ),
            label = "play pause",
        ) { playing ->
                if (state.isBuffering) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = playContentColor,
                )
            } else {
                Icon(
                    if (playing) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                    if (playing) "Pause" else "Play",
                    modifier = Modifier.size(30.dp),
                    tint = playContentColor,
                )
            }
        }
    }
}

@Composable
private fun VolumeControlRow(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    capsule: Boolean = false,
    frostedGlass: Boolean = false,
    showThumb: Boolean = true,
) {
    val rowModifier = if (capsule) Modifier else modifier
    val content: @Composable () -> Unit = {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ResonanceIcons.VolumeUp,
                contentDescription = "Music volume",
                tint = style.secondaryForeground,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            VolumeSlider(
                state = state,
                style = style,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.weight(1f),
                frostedGlass = frostedGlass,
                showThumb = showThumb,
            )
        }
    }
    if (capsule) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = style.controlContainer,
            shape = RoundedCornerShape(50),
        ) {
            Box(Modifier.padding(horizontal = 11.dp, vertical = 1.dp)) { content() }
        }
    } else {
        content()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VolumeSlider(
    state: ExternalMediaState,
    style: PlayerVisualStyle,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    frostedGlass: Boolean = false,
    showThumb: Boolean = true,
) {
    val maximumVolume = state.maxVolume.coerceAtLeast(1)
    val targetVolume = state.volume.coerceIn(0, maximumVolume).toFloat()

    // Stable drag state — dragVolume is only updated from external state when NOT dragging or settling,
    // preventing the "snap-back" that occurred when an external broadcast arrived mid-drag or right at release.
    var isDragging by remember { mutableStateOf(false) }
    var dragVolume by remember { mutableFloatStateOf(targetVolume) }
    var lastDispatchedVolume by remember { mutableStateOf<Int?>(null) }
    var settleUntilMs by remember { mutableLongStateOf(0L) }

    // Only sync external state to dragVolume while the user is not actively dragging or settling.
    LaunchedEffect(targetVolume) {
        val now = SystemClock.elapsedRealtime()
        if (!isDragging && now >= settleUntilMs) {
            dragVolume = targetVolume
        }
    }

    // Bug 1 fix: both branches of the if(liquidGlass) were identical (dead code).
    // Single critically-damped spring (DampingRatioNoBouncy = 1.0f, stiffness 2400f) for all
    // themes: instantly snaps to hardware volume steps with ZERO overshoot.
    val animatedVolume by animateFloatAsState(
        targetValue = if (isDragging) dragVolume else targetVolume,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 2400f,
        ),
        label = "hardware volume",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Performance fix: isSettling was computed from SystemClock on every recomposition frame,
    // meaning the slider could stay in "settling" state longer than needed because Compose
    // only re-composes when state changes, not when time elapses. A LaunchedEffect wakes the
    // composition at exactly the right moment so the slider exits settling state promptly.
    var isSettlingState by remember { mutableStateOf(false) }
    LaunchedEffect(settleUntilMs) {
        if (settleUntilMs > 0L) {
            isSettlingState = true
            val remaining = settleUntilMs - SystemClock.elapsedRealtime()
            if (remaining > 0) delay(remaining)
            isSettlingState = false
        }
    }
    val isSettling = isSettlingState && targetVolume != dragVolume
    val displayedVolume = if (isDragging || isSettling) dragVolume else animatedVolume
    val sliderColors = SliderDefaults.colors(
        thumbColor = style.primaryContainer,
        activeTrackColor = style.primaryContainer,
        inactiveTrackColor = style.sliderInactive,
        disabledThumbColor = style.secondaryForeground.copy(alpha = 0.38f),
        disabledActiveTrackColor = style.primaryContainer.copy(alpha = 0.38f),
        disabledInactiveTrackColor = style.sliderInactive.copy(alpha = 0.38f),
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed || isDragging) 1.10f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 1000f,
        ),
        label = "volume thumb press",
    )
    Slider(
        value = displayedVolume,
        onValueChange = { value ->
            isDragging = true
            val newVol = value.coerceIn(0f, maximumVolume.toFloat())
            dragVolume = newVol
            val intVol = newVol.roundToInt().coerceIn(0, maximumVolume)
            if (lastDispatchedVolume != intVol) {
                lastDispatchedVolume = intVol
                onVolumeChange(intVol)
            }
        },
        modifier = modifier
            .height(60.dp)
            .semantics {
                contentDescription = "Music volume"
                stateDescription = "${dragVolume.roundToInt()} of $maximumVolume"
            },
        enabled = state.connected,
        valueRange = 0f..maximumVolume.toFloat(),
        onValueChangeFinished = {
            isDragging = false
            settleUntilMs = SystemClock.elapsedRealtime() + 450L
            lastDispatchedVolume = null
        },
        colors = sliderColors,
        interactionSource = interactionSource,
        thumb = {
            if (showThumb) {
                // Official M3 Expressive slider thumb: prominent vertical pill handle with tactile scale
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 38.dp)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .shadow(3.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(style.primaryContainer),
                )
            }
        },
        track = {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            ) {
                val y = size.height / 2f
                val fraction = (displayedVolume / maximumVolume.toFloat()).coerceIn(0f, 1f)
                val activeX = size.width * fraction
                val trackGapPx = 4.dp.toPx() // M3 gap between active and inactive track
                val trackThickness = 16.dp.toPx()

                val activeColor = if (state.connected) {
                    style.primaryContainer
                } else {
                    style.primaryContainer.copy(alpha = 0.38f)
                }
                val inactiveColor = if (state.connected) {
                    style.sliderInactive
                } else {
                    style.sliderInactive.copy(alpha = 0.38f)
                }

                // Inactive track with gap
                val inactiveStart = (activeX + trackGapPx).coerceAtMost(size.width)
                if (inactiveStart < size.width) {
                    drawLine(
                        color = inactiveColor,
                        start = Offset(inactiveStart, y),
                        end = Offset(size.width, y),
                        strokeWidth = trackThickness,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }

                // Active track
                if (activeX > 0f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, y),
                        end = Offset((activeX - trackGapPx * 0.5f).coerceAtLeast(0f), y),
                        strokeWidth = trackThickness,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
        },
    )
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    style: PlayerVisualStyle,
    onClick: () -> Unit,
    size: Dp = 54.dp,
    containerColor: Color = style.controlContainer,
    contentColor: Color = style.foreground,
    liquidMotion: Boolean = false,
    expressiveMotion: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonShape = if (liquidMotion) CircleShape else RoundedCornerShape(28.dp)

    val hoverAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.40f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = 600f,
        ),
        label = "$description enable alpha",
    )
    // Hoisted above the liquidMotion branch so the composable call is unconditional (Compose rule).
    // frostedGlassInteractiveEnhanced has its own internal spring; this scale is only used in the non-liquid path.
    val nonLiquidScale by animateFloatAsState(
        targetValue = if (!liquidMotion && pressed && enabled) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 850f),
        label = "$description press scale",
    )

    // Bug 3 fix: liquidMotion buttons now use frostedGlassInteractiveEnhanced (same as the play button)
    // so all Liquid Glass transport controls get the luminance ripple + spring press physics.
    // Non-liquid buttons keep the simple spring scale animation.
    val baseModifier = if (liquidMotion) {
        Modifier
            .size(size)
            .graphicsLayer { alpha = hoverAlpha }
            .shadow(elevation = 0.dp, shape = buttonShape)
            .clip(buttonShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        style.primaryContainer.copy(alpha = 0.18f),
                        Color(0xFFD1E3F2).copy(alpha = 0.08f),
                        style.controlContainer.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(1.dp, Color(0xFFD2E3F0).copy(alpha = 0.28f), buttonShape)
            .frostedGlassInteractiveEnhanced(
                interactionSource = interaction,
                glowColor = style.primaryContainer,
                pressScale = 0.88f,
                enabled = enabled,
                shape = buttonShape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    } else {
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = nonLiquidScale
                scaleY = nonLiquidScale
                alpha = hoverAlpha
            }
            .shadow(
                elevation = 1.dp,
                shape = buttonShape,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = 0.24f),
            )
            .clip(buttonShape)
            .background(SolidColor(containerColor))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = contentColor,
        )
    }
}

@Composable
private fun EmptyPlayerState(state: ExternalMediaState, style: PlayerVisualStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = style.badgeContainer,
            shape = CircleShape,
        ) {
            Icon(
                ResonanceIcons.MusicNote,
                contentDescription = null,
                modifier = Modifier.padding(14.dp).size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                if (state.notificationAccessGranted) "Nothing is playing" else "Media access needed",
                color = style.foreground,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (state.notificationAccessGranted) {
                    "Start music in another app"
                } else {
                    "Grant access inside Resonance Material Control"
                },
                color = style.secondaryForeground,
            )
        }
    }
}
