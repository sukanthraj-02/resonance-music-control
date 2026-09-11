package com.sukanth.resonance.ui.setup

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukanth.resonance.lockscreen.ExternalMediaState
import com.sukanth.resonance.lockscreen.PlayerVisualTheme
import com.sukanth.resonance.ui.icons.ResonanceIcons
import com.sukanth.resonance.ui.theme.ResonanceTokens

@Composable
fun SetupScreen(
    state: ExternalMediaState,
    notificationAccessGranted: Boolean,
    accessibilityAccessGranted: Boolean,
    unrestrictedBatteryAccessGranted: Boolean,
    showAccessibilityDisclosure: Boolean,
    lockScreenEnabled: Boolean,
    automaticPlayerEnabled: Boolean,
    visualTheme: PlayerVisualTheme,
    onVisualThemeChange: (PlayerVisualTheme) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestUnrestrictedBatteryAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onDismissAccessibilityDisclosure: () -> Unit,
    onAcceptAccessibilityDisclosure: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAutomaticPlayerEnabledChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenSourceApp: () -> Unit,
) {
    val readyStepCount = listOf(
        notificationAccessGranted,
        accessibilityAccessGranted,
        unrestrictedBatteryAccessGranted,
    ).count { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = ResonanceTokens.Space.lg,
                vertical = ResonanceTokens.Space.md,
            ),
            verticalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.lg),
        ) {
            item {
                SetupHeader(
                    readyStepCount = readyStepCount,
                    lockScreenEnabled = lockScreenEnabled,
                )
            }
            item {
                CurrentSessionCard(
                    state = state,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onOpenSourceApp = onOpenSourceApp,
                )
            }
            item {
                SectionHeading(
                    eyebrow = "SETUP",
                    title = "Make Resonance Material Control ready",
                    supportingText = "$readyStepCount of 3 access steps complete",
                )
            }
            item {
                PermissionCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Media notification access",
                    description = "Reads active media controls, track details, and artwork.",
                    granted = notificationAccessGranted,
                    onClick = onRequestNotificationAccess,
                )
            }
            item {
                PermissionCard(
                    icon = Icons.Rounded.Person,
                    title = "Lock-screen display access",
                    description = "Shows your chosen controller above Android's secure keyguard.",
                    granted = accessibilityAccessGranted,
                    onClick = onRequestAccessibilityAccess,
                )
            }
            item {
                PermissionCard(
                    icon = Icons.Rounded.Check,
                    title = "Unrestricted battery access",
                    description = "Keeps the media bridge responsive while the screen is locked.",
                    granted = unrestrictedBatteryAccessGranted,
                    onClick = onRequestUnrestrictedBatteryAccess,
                )
            }
            item {
                LockScreenEnableCard(
                    enabled = lockScreenEnabled,
                    onEnabledChange = onEnabledChange,
                )
            }
            item {
                AutomaticPlayerCard(
                    enabled = automaticPlayerEnabled,
                    onEnabledChange = onAutomaticPlayerEnabledChange,
                )
            }
            item {
                ThemeSelectorCard(
                    selectedTheme = visualTheme,
                    onThemeSelect = onVisualThemeChange,
                )
            }
            item {
                PreviewButton(
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(ResonanceTokens.Shape.card),
                ) {
                    Row(
                        modifier = Modifier.padding(ResonanceTokens.Space.lg),
                        horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.md),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            ResonanceIcons.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = "Start music in another app first. Resonance Material Control is a controller, not an audio player.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(ResonanceTokens.Space.xl)) }
        }
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = onDismissAccessibilityDisclosure,
            icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            title = { Text("Allow lock-screen display access?") },
            text = {
                Text(
                            "Resonance Material Control uses Android's AccessibilityService only to place your " +
                        "chosen media controller over the real lock screen and mirror media " +
                        "volume while that controller is visible.\n\nIt does not read screen " +
                        "content, passwords, typed text, or other apps, and it cannot perform " +
                        "gestures. Song information comes separately from media notification access. " +
                        "On Android 13 and newer, Android may first show App Info: tap ⋮, choose " +
                        "Allow restricted settings, then enable Resonance in Accessibility.",
                )
            },
            confirmButton = {
                Button(onClick = onAcceptAccessibilityDisclosure) {
                    Text("Open permission settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAccessibilityDisclosure) {
                    Text("Not now")
                }
            },
        )
    }

}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    supportingText: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = ResonanceTokens.Space.sm),
        verticalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.xs),
    ) {
        Text(
            text = eyebrow,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AutomaticPlayerCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        shape = RoundedCornerShape(ResonanceTokens.Shape.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ResonanceTokens.Space.lg),
            horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "Open Resonance Material Control automatically",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(ResonanceTokens.Space.xs))
                Text(
                    text = if (enabled) {
                        "The expressive player opens when playback starts on the lock screen."
                    } else {
                        "Keep Android's native player visible and use the movable button to open it."
                    },
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun SetupHeader(
    readyStepCount: Int,
    lockScreenEnabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val heroShape = RoundedCornerShape(ResonanceTokens.Shape.hero)
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = heroShape,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = ResonanceTokens.Space.md, end = ResonanceTokens.Space.md)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer.copy(alpha = 0.72f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = ResonanceTokens.Space.xl, bottom = ResonanceTokens.Space.xl)
                    .size(width = 72.dp, height = 30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.tertiaryContainer.copy(alpha = 0.76f)),
            )
            Column(modifier = Modifier.padding(ResonanceTokens.Space.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            ResonanceIcons.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.padding(ResonanceTokens.Space.md),
                        )
                    }
                    Spacer(Modifier.width(ResonanceTokens.Space.md))
                    Text(
                        text = "RESONANCE MATERIAL CONTROL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = if (lockScreenEnabled) colors.tertiaryContainer else colors.surfaceContainerHigh,
                        contentColor = if (lockScreenEnabled) colors.onTertiaryContainer else colors.onSurfaceVariant,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = if (lockScreenEnabled) "Enabled" else "Setup",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(ResonanceTokens.Space.xxl))
                Text(
                    text = "Your music,\none tap away",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 38.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(ResonanceTokens.Space.sm))
                Text(
                    text = "A private, expressive controller that lives above Android's secure lock screen.",
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(0.82f),
                )
                Spacer(Modifier.height(ResonanceTokens.Space.xl))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.md),
                ) {
                    Row(
                        modifier = Modifier.width(92.dp),
                        horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.xs),
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < readyStepCount) colors.primary else colors.outlineVariant,
                                    ),
                            )
                        }
                    }
                    Text(
                        text = "$readyStepCount of 3 access steps ready",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentSessionCard(
    state: ExternalMediaState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenSourceApp: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val statusLabel = when {
        state.isBuffering -> "Buffering"
        state.isPlaying -> "Playing"
        state.connected -> "Paused"
        else -> "Waiting for music"
    }
    val sessionShape = RoundedCornerShape(ResonanceTokens.Shape.hero)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
        ),
        shape = sessionShape,
    ) {
        Column(modifier = Modifier.padding(ResonanceTokens.Space.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CURRENT SESSION",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (state.connected) colors.tertiaryContainer else colors.surfaceContainerHighest,
                    contentColor = if (state.connected) colors.onTertiaryContainer else colors.onSurfaceVariant,
                    shape = CircleShape,
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(ResonanceTokens.Space.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(bitmap = state.artwork, title = state.title)
                Spacer(Modifier.width(ResonanceTokens.Space.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(ResonanceTokens.Space.xs))
                    Text(
                        text = state.artist,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (state.connected) {
                Spacer(Modifier.height(ResonanceTokens.Space.lg))
                FilledTonalButton(
                    onClick = onOpenSourceApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(ResonanceTokens.Shape.control),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(ResonanceTokens.Space.sm))
                    Text(
                        text = "Open ${state.sourceApp}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(ResonanceTokens.Space.xl))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveTransportButton(
                        onClick = onPrevious,
                        enabled = state.canGoPrevious,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(26.dp),
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ) {
                        Icon(ResonanceIcons.SkipPrevious, contentDescription = "Previous")
                    }
                    ExpressiveTransportButton(
                        onClick = onPlayPause,
                        enabled = state.connected && state.canPlayPause,
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(30.dp),
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = colors.onPrimary,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) ResonanceIcons.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    ExpressiveTransportButton(
                        onClick = onNext,
                        enabled = state.canGoNext,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(26.dp),
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ) {
                        Icon(ResonanceIcons.SkipNext, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = ResonanceTokens.Motion.fastSpatial,
        label = "transport press scale",
    )
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        content = content,
    )
}

@Composable
private fun Artwork(bitmap: Bitmap?, title: String) {
    val shape = RoundedCornerShape(ResonanceTokens.Shape.card)
    val modifier = Modifier
        .size(104.dp)
        .clip(shape)
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Album artwork for $title",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ResonanceIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.semantics {
            stateDescription = if (granted) "Access granted" else "Access not granted"
        },
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer,
            contentColor = colors.onSurface,
        ),
        shape = RoundedCornerShape(ResonanceTokens.Shape.card),
    ) {
        Column(modifier = Modifier.padding(ResonanceTokens.Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    color = if (granted) colors.tertiaryContainer else colors.secondaryContainer,
                    contentColor = if (granted) colors.onTertiaryContainer else colors.onSecondaryContainer,
                    shape = RoundedCornerShape(ResonanceTokens.Shape.control),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(Modifier.width(ResonanceTokens.Space.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(ResonanceTokens.Space.xs))
                    Text(
                        text = description,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(ResonanceTokens.Space.lg))
            Surface(
                color = if (granted) colors.tertiaryContainer else colors.secondaryContainer,
                contentColor = if (granted) colors.onTertiaryContainer else colors.onSecondaryContainer,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Rounded.CheckCircle else ResonanceIcons.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (granted) "Ready" else "Action needed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(ResonanceTokens.Space.md))
            if (granted) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(ResonanceTokens.Shape.control),
                ) {
                    Text("Review access")
                }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(ResonanceTokens.Shape.control),
                ) {
                    Text("Grant access")
                }
            }
        }
    }
}

@Composable
private fun LockScreenEnableCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (enabled) colors.secondaryContainer else colors.surfaceContainerHigh,
        animationSpec = tween(ResonanceTokens.Motion.colorChangeMs),
        label = "enable card color",
    )
    Card(
        modifier = Modifier.semantics {
            stateDescription = if (enabled) "Lock-screen player enabled" else "Lock-screen player disabled"
        },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurface,
        ),
        shape = RoundedCornerShape(ResonanceTokens.Shape.hero),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ResonanceTokens.Space.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = if (enabled) colors.primary else colors.primaryContainer,
                contentColor = if (enabled) colors.onPrimary else colors.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(ResonanceTokens.Space.lg),
                )
            }
            Spacer(Modifier.width(ResonanceTokens.Space.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable lock-screen player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(ResonanceTokens.Space.xs))
                Text(
                    text = "Stays until you swipe it away. Your PIN or fingerprint is never bypassed.",
                    color = if (enabled) {
                        colors.onSecondaryContainer.copy(alpha = 0.76f)
                    } else {
                        colors.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(ResonanceTokens.Space.md))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier
                    .sizeIn(minWidth = 52.dp, minHeight = 48.dp)
                    .semantics {
                        stateDescription = if (enabled) "Enabled" else "Disabled"
                    },
            )
        }
    }
}

@Composable
private fun PreviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = ResonanceTokens.Motion.restrainedSpatial,
        label = "preview press scale",
    )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(ResonanceTokens.Shape.hero),
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null)
        Spacer(Modifier.width(ResonanceTokens.Space.md))
        Text(
            text = "Preview lock-screen player",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedTheme: PlayerVisualTheme,
    onThemeSelect: (PlayerVisualTheme) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer,
            contentColor = colors.onSurface,
        ),
        shape = RoundedCornerShape(ResonanceTokens.Shape.card),
    ) {
        Column(modifier = Modifier.padding(ResonanceTokens.Space.lg)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    shape = RoundedCornerShape(ResonanceTokens.Shape.control),
                ) {
                    Icon(
                        ResonanceIcons.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(Modifier.width(ResonanceTokens.Space.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lock-screen visual theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(ResonanceTokens.Space.xs))
                    Text(
                        text = "Choose your preferred layout and aesthetic.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(ResonanceTokens.Space.lg))
            // Vertical column of preview cards — one per theme
            Column(verticalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.sm)) {
                PlayerVisualTheme.selectableEntries.forEach { theme ->
                    ThemePreviewItem(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onSelect = { onThemeSelect(theme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewItem(
    theme: PlayerVisualTheme,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = ResonanceTokens.Motion.fastSpatial,
        label = "theme card press scale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.45f),
        animationSpec = tween(ResonanceTokens.Motion.colorChangeMs),
        label = "theme card border",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surfaceContainerHigh,
        animationSpec = tween(ResonanceTokens.Motion.colorChangeMs),
        label = "theme card bg",
    )
    // Capture outside the semantics lambda to avoid the property-setter shadowing the parameter.
    val isSelected = selected

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics {
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        shape = RoundedCornerShape(ResonanceTokens.Shape.hero),
        color = bgColor,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSelect,
                )
                .padding(ResonanceTokens.Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ResonanceTokens.Space.md),
        ) {
            // Mini visual preview thumbnail drawn via Canvas
            ThemePreviewThumbnail(
                theme = theme,
                selected = selected,
                modifier = Modifier
                    .size(width = 90.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            // Theme info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (selected) {
                        Surface(
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp),
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(ResonanceTokens.Space.xs))
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.80f) else colors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewThumbnail(
    theme: PlayerVisualTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val primaryContainer = colors.primaryContainer
    val surface = colors.surfaceContainerHigh
    val onSurface = colors.onSurface

    Canvas(modifier = modifier) {
        when (theme) {
            PlayerVisualTheme.FROSTED_GLASS -> {
                // iOS-18 frosted widgets over a soft blurred colour wash.
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A0A2E),
                            Color(0xFF0D1A2A),
                            Color(0xFF050810),
                        ),
                    ),
                )
                // Hero artwork square
                val artLeft = size.width * 0.22f
                val artRight = size.width * 0.78f
                val artTop = size.height * 0.12f
                val artSize = artRight - artLeft
                val artBottom = artTop + artSize
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF8060D0), Color(0xFF3040A0)),
                        startY = artTop,
                        endY = artBottom,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artTop),
                    size = androidx.compose.ui.geometry.Size(artSize, artSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                )
                // Floating frosted metadata pill
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f)),
                        startY = size.height * 0.28f,
                        endY = size.height * 0.36f,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                )
                // Transport dock with accent play orb
                val dotY = size.height * 0.86f
                val dotSpacing = (artRight - artLeft) / 4f
                for (i in 0..2) {
                    val cx = artLeft + dotSpacing * (i + 0.5f) + dotSpacing * 0.5f
                    val radius = if (i == 1) 9f else 5.5f
                    drawCircle(
                        color = if (i == 1) Color(0xFF7FA6FF) else Color.White.copy(alpha = 0.50f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(cx, dotY),
                    )
                }
            }

            PlayerVisualTheme.HI_FI_STUDIO -> {
                // Warm dark chassis with brushed chrome deck
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1A130E), Color(0xFF0B0908)),
                    ),
                )
                // Chrome card body
                val cardLeft = size.width * 0.10f
                val cardRight = size.width * 0.90f
                val cardTop = size.height * 0.10f
                val cardBottom = size.height * 0.90f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF2A221C), Color(0xFF14100D)),
                        startY = cardTop,
                        endY = cardBottom,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(cardLeft, cardTop),
                    size = androidx.compose.ui.geometry.Size(cardRight - cardLeft, cardBottom - cardTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                )
                // Brushed chrome top rail
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFE6E2DC).copy(alpha = 0.6f), Color.Transparent),
                        startX = cardLeft,
                        endX = cardRight,
                    ),
                    start = androidx.compose.ui.geometry.Offset(cardLeft + 8f, cardTop),
                    end = androidx.compose.ui.geometry.Offset(cardRight - 8f, cardTop),
                    strokeWidth = 1.5f,
                )
                // Vinyl artwork
                val artLeft = cardLeft + 8f
                val artRight = cardRight - 8f
                val artTop = cardTop + 10f
                val artBottom = artTop + (cardBottom - cardTop) * 0.5f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF8A5A2B), Color(0xFF3A2410)),
                        startY = artTop,
                        endY = artBottom,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artTop),
                    size = androidx.compose.ui.geometry.Size(artRight - artLeft, (cardBottom - cardTop) * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
                )
                // Warm title readout
                drawRoundRect(
                    color = Color(0xFFF3EBDC).copy(alpha = 0.85f),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artBottom - 34f),
                    size = androidx.compose.ui.geometry.Size((artRight - artLeft) * 0.62f, 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                )
                // VU meter ladder
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFC9892B), Color(0xFF8A5A2B)),
                        startY = artBottom - 22f,
                        endY = artBottom - 8f,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artBottom - 22f),
                    size = androidx.compose.ui.geometry.Size(artRight - artLeft, 14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f),
                )
                // Amber dial play
                drawCircle(
                    color = Color(0xFFC9892B),
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, cardBottom - 6f),
                )
            }

            PlayerVisualTheme.DUOTONE -> {
                // Bold duotone gradient wash
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFE070C0), Color(0xFF602080)),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    ),
                )
                // Duotone hero artwork
                val artLeft = size.width * 0.12f
                val artRight = size.width * 0.88f
                val artTop = size.height * 0.14f
                val artSize = artRight - artLeft
                val artBottom = artTop + artSize
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF402080), Color(0xFFC060A0)),
                        start = androidx.compose.ui.geometry.Offset(artLeft, artTop),
                        end = androidx.compose.ui.geometry.Offset(artRight, artBottom),
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artTop),
                    size = androidx.compose.ui.geometry.Size(artSize, artSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                )
                // Bold editorial title
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.12f, artBottom + 8f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, 4.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.12f, artBottom + 16f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.40f, 3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f),
                )
                // Rounded transport pills
                val dotY = size.height * 0.88f
                val dotSpacing = (artRight - artLeft) / 4f
                for (i in 0..2) {
                    val cx = artLeft + dotSpacing * (i + 0.5f) + dotSpacing * 0.5f
                    if (i == 1) {
                        drawCircle(color = Color.White, radius = 9f, center = androidx.compose.ui.geometry.Offset(cx, dotY))
                    } else {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = androidx.compose.ui.geometry.Offset(cx - 7f, dotY - 5.5f),
                            size = androidx.compose.ui.geometry.Size(14f, 11f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.5f),
                        )
                    }
                }
            }

            PlayerVisualTheme.MATERIAL_3_EXPRESSIVE -> {
                // Tonal background
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            primaryContainer.copy(alpha = 0.20f).compositeOver(surface),
                            surface,
                        ),
                    ),
                )
                // Player card (rounded surface card)
                val cardLeft = size.width * 0.08f
                val cardRight = size.width * 0.92f
                val cardTop = size.height * 0.08f
                val cardBottom = size.height * 0.92f
                val cardCorner = 14f
                drawRoundRect(
                    color = primaryContainer.copy(alpha = 0.30f).compositeOver(surface),
                    topLeft = androidx.compose.ui.geometry.Offset(cardLeft, cardTop),
                    size = androidx.compose.ui.geometry.Size(cardRight - cardLeft, cardBottom - cardTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cardCorner),
                )
                // Artwork rounded square
                val artLeft = cardLeft + 8f
                val artRight = cardRight - 8f
                val artTop = cardTop + 8f
                val artBottom = artTop + (cardBottom - cardTop) * 0.44f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.90f), primaryContainer.copy(alpha = 0.85f)),
                        startY = artTop,
                        endY = artBottom,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artTop),
                    size = androidx.compose.ui.geometry.Size(artRight - artLeft, artBottom - artTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                )
                // Title line
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.82f),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artBottom + 6f),
                    size = androidx.compose.ui.geometry.Size((artRight - artLeft) * 0.65f, 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                )
                // Subtitle line
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.38f),
                    topLeft = androidx.compose.ui.geometry.Offset(artLeft, artBottom + 14f),
                    size = androidx.compose.ui.geometry.Size((artRight - artLeft) * 0.42f, 3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f),
                )
                // Transport buttons
                val dotY = cardBottom - 14f
                val dotSpacing = (artRight - artLeft) / 4f
                for (i in 0..2) {
                    val cx = artLeft + dotSpacing * (i + 0.5f) + dotSpacing * 0.5f
                    val w = if (i == 1) 18f else 12f
                    val h = if (i == 1) 18f else 12f
                    drawRoundRect(
                        color = if (i == 1) primary else primaryContainer.copy(alpha = 0.70f),
                        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2f, dotY - h / 2f),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(if (i == 1) 6f else 4f),
                    )
                }
            }
        }
    }
}

