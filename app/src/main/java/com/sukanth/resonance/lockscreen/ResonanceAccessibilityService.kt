package com.sukanth.resonance.lockscreen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sukanth.resonance.ui.lockscreen.LockScreenPlayerScreen
import com.sukanth.resonance.ui.icons.ResonanceIcons
import com.sukanth.resonance.ui.theme.ResonanceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps Android's native lock-screen media card visible and adds a small Material 3 Expressive
 * launcher over an empty part of the keyguard. The custom player is expanded only after the user
 * taps that launcher. It never requests window content, traverses UI nodes, performs gestures, or
 * keeps a wake lock. Media data comes exclusively from [MediaAccessService].
 */
class ResonanceAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val isExiting = mutableStateOf(false)
    private var overlayView: ComposeView? = null
    private var overlayOwner: OverlayLifecycleOwner? = null
    private var outgoingOverlayView: ComposeView? = null
    private var outgoingOverlayOwner: OverlayLifecycleOwner? = null
    private var overlayTransitioning = false
    private var receiverRegistered = false
    private var volumeObserverRegistered = false
    private var screenInteractive = false
    private var dismissedForCurrentLock = false
    private var keepOverlayWarm = false
    private var customPlayerExpanded = false
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var callResumeAttempts = 0
    private var keyguardSettleAttempts = 0

    private val removeOverlay = Runnable(::removeOverlayNow)
    private val reevaluateOverlay = Runnable(::updateOverlayVisibility)
    private val resumeAfterCall = Runnable(::resumeOverlayAfterCall)
    private val reconcileVolume = Runnable(MediaSessionMonitor::refreshVolume)
    private val keyguardGuard = Runnable(::guardLockScreenVisibility)

    private val volumeObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            mainHandler.removeCallbacks(reconcileVolume)
            mainHandler.postDelayed(reconcileVolume, VOLUME_OBSERVER_SETTLE_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenInteractive = false
                    mainHandler.removeCallbacks(resumeAfterCall)
                    mainHandler.removeCallbacks(reevaluateOverlay)
                    callResumeAttempts = 0
                    keyguardSettleAttempts = 0
                    dismissedForCurrentLock = false
                    // Keep a small launcher composed while the display is off. That means it is
                    // ready before the keyguard starts drawing on the next wake, rather than
                    // paying the first-Compose cost after the lock screen is already visible.
                    if (customPlayerExpanded || overlayTransitioning) removeOverlayNow()
                    keepOverlayWarm = LockScreenPreferences.isEnabled(this@ResonanceAccessibilityService)
                    if (keepOverlayWarm) {
                        suspendOverlay()
                        prewarmOverlay()
                    } else {
                        suspendOverlay()
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    screenInteractive = true
                    dismissedForCurrentLock = false
                    keyguardSettleAttempts = 0
                    // KeyguardManager can report false for several frames after SCREEN_ON even
                    // though the device is waking directly to the lock screen. Keep the already
                    // composed launcher warm during that hand-off instead of tearing it down.
                    keepOverlayWarm = LockScreenPreferences.isEnabled(this@ResonanceAccessibilityService)
                    if (keepOverlayWarm) prewarmOverlay()
                    // Use the warm, previously-known state for an instant launcher reveal. The
                    // controller refresh follows on the next main-loop turn and reconciles any
                    // state that changed while the display was off.
                    updateOverlayVisibility()
                    mainHandler.post {
                        MediaSessionMonitor.refreshSessions()
                        updateOverlayVisibility()
                    }
                    // A few OEMs update KeyguardManager just after ACTION_SCREEN_ON.
                    mainHandler.removeCallbacks(reevaluateOverlay)
                    mainHandler.postDelayed(reevaluateOverlay, KEYGUARD_SETTLE_MS)
                }

                Intent.ACTION_USER_PRESENT -> {
                    // The launcher is only valid on the keyguard. Removing it immediately
                    // avoids the small FAB flashing over the first unlocked frame while the
                    // old exit animation is still running.
                    dismissedForCurrentLock = true
                    keyguardSettleAttempts = 0
                    mainHandler.removeCallbacks(reevaluateOverlay)
                    removeOverlayNow()
                }

                LockScreenPreferences.ACTION_AUTOMATIC_PLAYER_CHANGED -> {
                    if (!LockScreenPreferences.isAutomaticPlayerEnabled(this@ResonanceAccessibilityService)) {
                        collapseCustomPlayer()
                    }
                    updateOverlayVisibility()
                }

                android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val stateStr = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                    if (stateStr == android.telephony.TelephonyManager.EXTRA_STATE_IDLE) {
                        // Do not include the cached monitor flag here: it is intentionally true
                        // throughout the call and must be cleared by this explicit end event.
                        MediaSessionMonitor.onCallStateChanged(false)
                        scheduleOverlayResumeAfterCall()
                    } else {
                        val isRingingOrInCall =
                            stateStr == android.telephony.TelephonyManager.EXTRA_STATE_RINGING ||
                                stateStr == android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK ||
                                isCallActive()
                        MediaSessionMonitor.onCallStateChanged(isRingingOrInCall)
                        if (isRingingOrInCall) {
                            mainHandler.removeCallbacks(resumeAfterCall)
                            callResumeAttempts = 0
                            hideOverlay(animate = false)
                        } else {
                            scheduleOverlayResumeAfterCall()
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Observe volume key-down events without consuming them. Android's own audio pipeline
        // still changes the stream, while the slider can move on the same input frame.
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        MediaSessionMonitor.initialize(this)
        MediaSessionMonitor.connect()
        screenInteractive = getSystemService(PowerManager::class.java).isInteractive
        registerScreenReceiver()

        serviceScope.launch {
            MediaSessionMonitor.state
                .map { Triple(it.callActive, it.isLockScreenEligible, it.isPlaybackActive) }
                .distinctUntilChanged()
                .collect { (callActive, _, _) ->
                    // Calls must take precedence over the player surface immediately. Do not
                    // leave the accessibility overlay over the incoming-call UI animation.
                    if (callActive) hideOverlay(animate = false)
                    updateOverlayVisibility()
                }
        }
        updateOverlayVisibility()
    }

    /** Accessibility events are intentionally ignored; no app/window content is inspected. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (overlayView == null || !isKeyguardLocked()) {
            return false
        }
        val isVolumeKey = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> false
        }
        if (!isVolumeKey) return false
        if (isCallActive()) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> MediaSessionMonitor.optimisticVolumeStep(AudioManager.ADJUST_RAISE)
                KeyEvent.KEYCODE_VOLUME_DOWN -> MediaSessionMonitor.optimisticVolumeStep(AudioManager.ADJUST_LOWER)
            }
        }

        // Return false so Android's native audio policy processes volume key events
        // symmetrically (both ACTION_DOWN and ACTION_UP) without stuck repeat states.
        // MediaSessionMonitor's broadcast receiver and system observer pick up the resulting
        // volume change cleanly.
        return false
    }

    override fun onInterrupt() {
        hideOverlay(animate = false)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(removeOverlay)
        mainHandler.removeCallbacks(reevaluateOverlay)
        mainHandler.removeCallbacks(resumeAfterCall)
        mainHandler.removeCallbacks(keyguardGuard)
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        unregisterVolumeObserver()
        removeOverlayNow()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun isCallActive(includeMonitorState: Boolean = true): Boolean = runCatching {
        val audioManager = getSystemService(AudioManager::class.java)
        val telephonyManager = getSystemService(android.telephony.TelephonyManager::class.java)
        val audioModeActive = when (audioManager?.mode) {
            AudioManager.MODE_RINGTONE,
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            -> true
            else -> false
        }
        val telephonyActive = runCatching {
            @Suppress("DEPRECATION")
            telephonyManager?.callState != android.telephony.TelephonyManager.CALL_STATE_IDLE
        }.getOrDefault(false)

        audioModeActive || telephonyActive ||
            (includeMonitorState && MediaSessionMonitor.state.value.callActive)
    }.getOrDefault(
        if (includeMonitorState) MediaSessionMonitor.state.value.callActive else false,
    )

    private fun scheduleOverlayResumeAfterCall() {
        mainHandler.removeCallbacks(resumeAfterCall)
        callResumeAttempts = 0
        mainHandler.postDelayed(resumeAfterCall, CALL_RESUME_INITIAL_DELAY_MS)
    }

    private fun resumeOverlayAfterCall() {
        // Telephony broadcasts can arrive before AudioManager leaves MODE_IN_CALL. Retry briefly
        // instead of reopening the player over the tail of the system call UI animation.
        if (isCallActive(includeMonitorState = false)) {
            if (callResumeAttempts++ < CALL_RESUME_MAX_ATTEMPTS) {
                mainHandler.postDelayed(resumeAfterCall, CALL_RESUME_RETRY_MS)
            }
            return
        }
        callResumeAttempts = 0
        MediaSessionMonitor.onCallStateChanged(false)
        MediaSessionMonitor.refreshSessions()
        updateOverlayVisibility()
    }

    private fun updateOverlayVisibility() {
        // Some OEMs do not deliver SCREEN_ON consistently when waking directly to keyguard.
        // Read the current state as well as relying on the broadcast-maintained value.
        screenInteractive = getSystemService(PowerManager::class.java).isInteractive
        val callActive = isCallActive()
        if (callActive) {
            MediaSessionMonitor.onCallStateChanged(true)
            hideOverlay(animate = false)
            return
        }

        val mediaState = MediaSessionMonitor.state.value
        val automaticPlayer = LockScreenPreferences.isAutomaticPlayerEnabled(this)
        val keyguardLocked = isKeyguardLocked()
        val shouldShow =
            screenInteractive &&
                !dismissedForCurrentLock &&
                keyguardLocked &&
                LockScreenPreferences.isEnabled(this) &&
                mediaState.isLockScreenEligible &&
                (!automaticPlayer || mediaState.isPlaybackActive)
        if (shouldShow) {
            keyguardSettleAttempts = 0
            showOverlay()
        } else if (
            keepOverlayWarm &&
                screenInteractive.not() &&
                !dismissedForCurrentLock
        ) {
            suspendOverlay()
        } else if (
            keepOverlayWarm &&
                screenInteractive &&
                !dismissedForCurrentLock &&
                LockScreenPreferences.isEnabled(this)
        ) {
            // Preserve the prewarmed composition during Android's short screen-on/keyguard
            // race. Poll rather than waiting for an unrelated media callback, so the FAB becomes
            // visible as soon as the keyguard is actually reported.
            prewarmOverlay()
            if (!keyguardLocked) scheduleKeyguardSettleCheck()
        } else {
            keepOverlayWarm = false
            hideOverlay(animate = overlayView != null)
        }
    }

    private fun scheduleKeyguardSettleCheck() {
        if (keyguardSettleAttempts >= KEYGUARD_SETTLE_MAX_ATTEMPTS) return
        keyguardSettleAttempts += 1
        mainHandler.removeCallbacks(reevaluateOverlay)
        mainHandler.postDelayed(reevaluateOverlay, KEYGUARD_SETTLE_RETRY_MS)
    }

    private fun prewarmOverlay() {
        if (overlayView == null) {
            attachOverlay(visible = false)
        } else if (overlayView?.visibility != View.VISIBLE) {
            overlayView?.visibility = View.INVISIBLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showOverlay() {
        mainHandler.removeCallbacks(removeOverlay)
        isExiting.value = false
        // The incoming full-screen surface owns visibility until its first pre-draw. A state
        // refresh during those few milliseconds must not reveal the transparent surface early.
        if (overlayTransitioning) return
        overlayView?.let {
            if (LockScreenPreferences.isAutomaticPlayerEnabled(this) && !customPlayerExpanded) {
                expandCustomPlayer()
            } else {
                // A pre-warmed launcher may have been hidden during a prior collapse. Restore it
                // immediately—there is no entrance animation on the small FAB.
                it.animate().cancel()
                it.visibility = View.VISIBLE
                it.alpha = 1f
                it.isEnabled = true
            }
            registerVolumeObserver()
            scheduleKeyguardGuard()
            return
        }

        attachOverlay(visible = true)
    }

    @Suppress("DEPRECATION")
    private fun attachOverlay(visible: Boolean) {
        mainHandler.removeCallbacks(removeOverlay)
        isExiting.value = false
        customPlayerExpanded = LockScreenPreferences.isAutomaticPlayerEnabled(this)
        overlayTransitioning = false

        val owner = OverlayLifecycleOwner()
        val view = ComposeView(this).apply {
            visibility = if (visible) View.VISIBLE else View.INVISIBLE
            alpha = 1f
            setBackgroundColor(Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            filterTouchesWhenObscured = true
            systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            setOverlayContent(this, expanded = customPlayerExpanded)
        }
        val layoutParams = createOverlayLayoutParams(expanded = customPlayerExpanded)

        runCatching {
            getSystemService(WindowManager::class.java).addView(view, layoutParams)
            overlayOwner = owner
            overlayView = view
            overlayLayoutParams = layoutParams
            registerVolumeObserver()
            scheduleKeyguardGuard()
            owner.start()
        }.onFailure {
            owner.destroy()
            overlayOwner = null
            overlayView = null
            overlayLayoutParams = null
        }
    }

    /**
     * Replaces the full-screen player with the small launcher. Because the collapsed overlay is a
     * WRAP_CONTENT window, touches outside the FAB continue to reach SystemUI's native player.
     */
    private fun collapseCustomPlayer() {
        if (!customPlayerExpanded || overlayTransitioning) return
        switchOverlay(expanded = false)
    }

    private fun expandCustomPlayer() {
        if (customPlayerExpanded || overlayTransitioning) return
        switchOverlay(expanded = true)
    }

    private fun moveFabBy(deltaX: Float, deltaY: Float) {
        if (customPlayerExpanded || overlayTransitioning) return
        val view = overlayView ?: return
        val params = overlayLayoutParams ?: return
        val density = resources.displayMetrics.density
        val windowSize = dp(FAB_WINDOW_SIZE_DP)
        val maxX = (resources.displayMetrics.widthPixels - windowSize).coerceAtLeast(0)
        val maxY = (resources.displayMetrics.heightPixels - windowSize).coerceAtLeast(0)

        // With END gravity, dragging right reduces the distance from the right edge.
        params.x = (params.x - deltaX.roundToInt()).coerceIn(0, maxX)
        params.y = (params.y + deltaY.roundToInt()).coerceIn(0, maxY)
        runCatching {
            getSystemService(WindowManager::class.java).updateViewLayout(view, params)
            LockScreenPreferences.setFabPositionDp(
                this,
                (params.x / density).roundToInt(),
                (params.y / density).roundToInt(),
            )
        }
    }

    private fun setOverlayContent(view: ComposeView, expanded: Boolean) {
        view.setContent {
            if (expanded) {
                val state by MediaSessionMonitor.state.collectAsState()
                val visualTheme = remember { LockScreenPreferences.getVisualTheme(this@ResonanceAccessibilityService) }
                ResonanceTheme(darkTheme = true) {
                    LockScreenPlayerScreen(
                        state = state,
                        visualTheme = visualTheme,
                        isExiting = isExiting.value,
                        onPlayPause = MediaSessionMonitor::togglePlayPause,
                        onPrevious = MediaSessionMonitor::previous,
                        onNext = MediaSessionMonitor::next,
                        onPlaylistItemSelected = MediaSessionMonitor::playPlaylistEntry,
                        onOpenSourceApp = MediaSessionMonitor::openSourceApp,
                        onSeek = MediaSessionMonitor::seekTo,
                        onVolumeChange = MediaSessionMonitor::setVolume,
                        onLike = MediaSessionMonitor::toggleLike,
                        onShuffle = MediaSessionMonitor::toggleShuffle,
                        onUnlock = ::dismissForCurrentLock,
                        onClose = ::collapseCustomPlayer,
                    )
                }
            } else {
                ResonanceTheme(darkTheme = true) {
                    MaterialPlayerLauncher(
                        onClick = ::expandCustomPlayer,
                        onMove = ::moveFabBy,
                    )
                }
            }
        }
    }

    /**
     * Never resize an attached FAB window into a player window. Several OEM WindowManagers place
     * the outgoing 72dp surface at (0, 0) for a frame during that resize. Preparing a separate
     * full/small window avoids that frame altogether and lets the old surface remain correctly
     * positioned until the replacement has completed its first layout.
     */
    private fun switchOverlay(expanded: Boolean) {
        val outgoingView = overlayView ?: run {
            customPlayerExpanded = expanded
            attachOverlay(visible = true)
            return
        }
        val outgoingOwner = overlayOwner
        overlayTransitioning = true
        isExiting.value = false

        val incomingOwner = OverlayLifecycleOwner()
        val incomingView = ComposeView(this).apply {
            // Alpha zero still receives measure/layout/pre-draw callbacks; INVISIBLE does not on
            // all OEMs. It gives us a guaranteed fully rendered first frame without exposing it.
            visibility = View.VISIBLE
            alpha = 0f
            isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(incomingOwner)
            setViewTreeSavedStateRegistryOwner(incomingOwner)
            setViewTreeViewModelStoreOwner(incomingOwner)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            filterTouchesWhenObscured = true
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            setOverlayContent(this, expanded = expanded)
        }
        val incomingParams = createOverlayLayoutParams(expanded)

        runCatching {
            getSystemService(WindowManager::class.java).addView(incomingView, incomingParams)
        }.onSuccess {
            outgoingOverlayView = outgoingView
            outgoingOverlayOwner = outgoingOwner
            overlayView = incomingView
            overlayOwner = incomingOwner
            overlayLayoutParams = incomingParams
            customPlayerExpanded = expanded
            incomingOwner.start()
            if (expanded) registerVolumeObserver() else unregisterVolumeObserver()

            incomingView.doOnPreDraw {
                if (overlayView !== incomingView || customPlayerExpanded != expanded) return@doOnPreDraw
                removeOutgoingOverlay()
                overlayTransitioning = false
                incomingView.isEnabled = true
                // A short alpha-only fade is cheap to composite and lets the full UI appear as a
                // single surface, rather than a stack of expensive entrance animations.
                incomingView.animate()
                    .alpha(1f)
                    .setDuration(OVERLAY_REVEAL_MS)
                    .start()
            }
        }.onFailure {
            incomingOwner.destroy()
            overlayTransitioning = false
        }
    }

    private fun removeOutgoingOverlay() {
        val outgoingView = outgoingOverlayView ?: return
        outgoingOverlayView = null
        runCatching { getSystemService(WindowManager::class.java).removeViewImmediate(outgoingView) }
        outgoingOverlayOwner?.destroy()
        outgoingOverlayOwner = null
    }

    @Suppress("DEPRECATION")
    private fun createOverlayLayoutParams(expanded: Boolean): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        return WindowManager.LayoutParams(
            if (expanded) WindowManager.LayoutParams.MATCH_PARENT else dp(FAB_WINDOW_SIZE_DP),
            if (expanded) WindowManager.LayoutParams.MATCH_PARENT else dp(FAB_WINDOW_SIZE_DP),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            if (expanded) flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = if (expanded) Gravity.FILL else Gravity.TOP or Gravity.END
            if (expanded) {
                // Do not inherit a previous launcher coordinate during a full-screen resize.
                x = 0
                y = 0
            } else {
                // Default to the open area just above the native media card. The position is
                // saved after a drag, so users can move it anywhere on the keyguard.
                val (savedX, savedY) = LockScreenPreferences.fabPositionDp(
                    this@ResonanceAccessibilityService,
                )
                x = dp(savedX)
                y = dp(savedY)
            }
            // System status icons remain owned by SystemUI; this only allows the expanded player
            // to draw under the protected top area when the user explicitly opens it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            title = if (expanded) {
                "Resonance Material Control expanded player"
            } else {
                "Open Resonance Material Control"
            }
        }
    }

    private fun updateOverlayLayout(expanded: Boolean) {
        val view = overlayView ?: return
        val params = createOverlayLayoutParams(expanded)
        runCatching {
            getSystemService(WindowManager::class.java).updateViewLayout(view, params)
            overlayLayoutParams = params
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun scheduleKeyguardGuard() {
        mainHandler.removeCallbacks(keyguardGuard)
        mainHandler.postDelayed(keyguardGuard, KEYGUARD_GUARD_INTERVAL_MS)
    }

    private fun guardLockScreenVisibility() {
        if (overlayView == null || !screenInteractive) return
        if (customPlayerExpanded) {
            // Do not collapse an actively opened player because KeyguardManager briefly reports
            // false during the screen-on hand-off. ACTION_USER_PRESENT and SCREEN_OFF still
            // remove it immediately; otherwise the user's swipe owns dismissal.
            scheduleKeyguardGuard()
            return
        }
        if (!isKeyguardLocked()) {
            dismissedForCurrentLock = true
            removeOverlayNow()
        } else {
            scheduleKeyguardGuard()
        }
    }

    private fun suspendOverlay() {
        mainHandler.removeCallbacks(removeOverlay)
        mainHandler.removeCallbacks(keyguardGuard)
        unregisterVolumeObserver()
        // A transition cannot finish while the display is off because an invisible root may not
        // receive pre-draw. Dispose the outgoing window now instead of retaining it until wake.
        removeOutgoingOverlay()
        overlayTransitioning = false
        overlayView?.apply {
            animate().cancel()
            alpha = 1f
            isEnabled = false
            visibility = View.INVISIBLE
        }
    }

    private fun dismissForCurrentLock() {
        dismissedForCurrentLock = true
        hideOverlay(animate = true)
    }

    private fun hideOverlay(animate: Boolean) {
        val view = overlayView ?: return
        mainHandler.removeCallbacks(removeOverlay)
        mainHandler.removeCallbacks(keyguardGuard)
        if (!animate || !screenInteractive) {
            removeOverlayNow()
            return
        }
        isExiting.value = true
        view.isEnabled = false
        mainHandler.postDelayed(removeOverlay, EXIT_ANIMATION_MS)
    }

    private fun removeOverlayNow() {
        val view = overlayView
        val owner = overlayOwner
        mainHandler.removeCallbacks(keyguardGuard)
        overlayView = null
        overlayLayoutParams = null
        customPlayerExpanded = false
        overlayTransitioning = false
        keyguardSettleAttempts = 0
        keepOverlayWarm = false
        unregisterVolumeObserver()
        removeOutgoingOverlay()
        view?.let { runCatching { getSystemService(WindowManager::class.java).removeViewImmediate(it) } }
        owner?.destroy()
        overlayOwner = null
        isExiting.value = false
    }

    private fun isKeyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(LockScreenPreferences.ACTION_AUTOMATIC_PLAYER_CHANGED)
            addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun registerVolumeObserver() {
        if (volumeObserverRegistered) return
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver,
        )
        volumeObserverRegistered = true
        MediaSessionMonitor.refreshVolume()
    }

    private fun unregisterVolumeObserver() {
        mainHandler.removeCallbacks(reconcileVolume)
        if (!volumeObserverRegistered) return
        contentResolver.unregisterContentObserver(volumeObserver)
        volumeObserverRegistered = false
    }

    private companion object {
        const val EXIT_ANIMATION_MS = 280L
        const val KEYGUARD_SETTLE_MS = 20L
        const val KEYGUARD_SETTLE_RETRY_MS = 40L
        const val KEYGUARD_SETTLE_MAX_ATTEMPTS = 30
        const val CALL_RESUME_INITIAL_DELAY_MS = 80L
        const val CALL_RESUME_RETRY_MS = 200L
        const val CALL_RESUME_MAX_ATTEMPTS = 20
        const val VOLUME_OBSERVER_SETTLE_MS = 24L
        const val OVERLAY_REVEAL_MS = 90L
        const val FAB_WINDOW_SIZE_DP = 72
        const val KEYGUARD_GUARD_INTERVAL_MS = 420L
    }
}

@androidx.compose.runtime.Composable
private fun MaterialPlayerLauncher(
    onClick: () -> Unit,
    onMove: (Float, Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ) {
            Icon(
                imageVector = ResonanceIcons.QueueMusic,
                contentDescription = "Open Resonance Material Control",
            )
        }
    }
}

private class OverlayLifecycleOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
