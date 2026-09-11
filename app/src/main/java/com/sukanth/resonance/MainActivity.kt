package com.sukanth.resonance

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukanth.resonance.lockscreen.ExternalPlayerViewModel
import com.sukanth.resonance.lockscreen.LockScreenPreferences
import com.sukanth.resonance.lockscreen.MediaAccessService
import com.sukanth.resonance.lockscreen.PlayerVisualTheme
import com.sukanth.resonance.ui.setup.SetupScreen
import com.sukanth.resonance.ui.theme.ResonanceTheme

class MainActivity : ComponentActivity() {
    private val playerViewModel: ExternalPlayerViewModel by viewModels()
    private var permissionRefresh by mutableIntStateOf(0)
    private var lockScreenEnabled by mutableStateOf(false)
    private var automaticPlayerEnabled by mutableStateOf(false)
    private var visualTheme by mutableStateOf(PlayerVisualTheme.MATERIAL_3_EXPRESSIVE)
    private var showAccessibilityDisclosure by mutableStateOf(false)
    private var enableAfterPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lockScreenEnabled = LockScreenPreferences.isEnabled(this)
        automaticPlayerEnabled = LockScreenPreferences.isAutomaticPlayerEnabled(this)
        visualTheme = LockScreenPreferences.getVisualTheme(this)

        setContent {
            val state by playerViewModel.state.collectAsStateWithLifecycle()
            @Suppress("UNUSED_VARIABLE")
            val refreshMarker = permissionRefresh
            val notificationAccess = playerViewModel.hasNotificationAccess()
            val accessibilityAccess = playerViewModel.hasAccessibilityAccess()
            val unrestrictedBatteryAccess = playerViewModel.hasUnrestrictedBatteryAccess()
            ResonanceTheme(darkTheme = true) {
                SetupScreen(
                    state = state,
                    notificationAccessGranted = notificationAccess,
                    accessibilityAccessGranted = accessibilityAccess,
                    unrestrictedBatteryAccessGranted = unrestrictedBatteryAccess,
                    showAccessibilityDisclosure = showAccessibilityDisclosure,
                    lockScreenEnabled = lockScreenEnabled,
                    automaticPlayerEnabled = automaticPlayerEnabled,
                    visualTheme = visualTheme,
                    onVisualThemeChange = { theme ->
                        LockScreenPreferences.setVisualTheme(this, theme)
                        visualTheme = theme
                    },
                    onRequestNotificationAccess = ::openNotificationAccess,
                    onRequestUnrestrictedBatteryAccess = ::openUnrestrictedBatteryAccess,
                    onRequestAccessibilityAccess = {
                        requestAccessibilityAccess(accessibilityAccess)
                    },
                    onDismissAccessibilityDisclosure = {
                        showAccessibilityDisclosure = false
                    },
                    onAcceptAccessibilityDisclosure = {
                        LockScreenPreferences.setAccessibilityDisclosureAccepted(this, true)
                        showAccessibilityDisclosure = false
                        openAccessibilityAccess()
                    },
                    onEnabledChange = { requested ->
                        enableAfterPermissions = requested
                        when {
                            !requested -> {
                                enableAfterPermissions = false
                                persistLockScreenEnabled(false)
                            }
                            !notificationAccess -> openNotificationAccess()
                            !accessibilityAccess -> requestAccessibilityAccess(accessibilityAccess)
                            else -> {
                                enableAfterPermissions = false
                                persistLockScreenEnabled(true)
                            }
                        }
                    },
                    onAutomaticPlayerEnabledChange = { enabled ->
                        LockScreenPreferences.setAutomaticPlayerEnabled(this, enabled)
                        automaticPlayerEnabled = enabled
                    },
                    onPreview = {
                        startActivity(LockScreenActivity.createIntent(this, preview = true))
                    },
                    onPlayPause = playerViewModel::togglePlayback,
                    onPrevious = playerViewModel::previous,
                    onNext = playerViewModel::next,
                    onOpenSourceApp = playerViewModel::openSourceApp,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lockScreenEnabled = LockScreenPreferences.isEnabled(this)
        automaticPlayerEnabled = LockScreenPreferences.isAutomaticPlayerEnabled(this)
        visualTheme = LockScreenPreferences.getVisualTheme(this)
        permissionRefresh++
        playerViewModel.refresh()
        if (
            enableAfterPermissions &&
            playerViewModel.hasNotificationAccess() &&
            playerViewModel.hasAccessibilityAccess()
        ) {
            enableAfterPermissions = false
            persistLockScreenEnabled(true)
        } else if (
            enableAfterPermissions &&
            playerViewModel.hasNotificationAccess() &&
            !playerViewModel.hasAccessibilityAccess()
        ) {
            // Returning from Accessibility without enabling the service should not immediately
            // reopen the same explanation. The user can tap the access card again when ready.
            enableAfterPermissions = false
            if (!LockScreenPreferences.isAccessibilityDisclosureAccepted(this)) {
                showAccessibilityDisclosure = true
            }
        }
    }

    private fun persistLockScreenEnabled(enabled: Boolean) {
        LockScreenPreferences.setEnabled(this, enabled)
        lockScreenEnabled = enabled
    }

    private fun openNotificationAccess() {
        val component = ComponentName(this, MediaAccessService::class.java)
        val accessIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString(),
                )
            }
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        runCatching { startActivity(accessIntent) }
            .onFailure { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    private fun openAccessibilityAccess() = openAccessibilitySettings()

    private fun requestAccessibilityAccess(accessGranted: Boolean) {
        if (accessGranted || LockScreenPreferences.isAccessibilityDisclosureAccepted(this)) {
            openAccessibilityAccess()
        } else {
            showAccessibilityDisclosure = true
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openUnrestrictedBatteryAccess() {
        runCatching {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.onFailure {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:$packageName".toUri(),
                ),
            )
        }
    }

}
