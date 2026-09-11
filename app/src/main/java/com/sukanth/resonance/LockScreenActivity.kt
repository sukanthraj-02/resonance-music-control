package com.sukanth.resonance

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukanth.resonance.lockscreen.ExternalPlayerViewModel
import com.sukanth.resonance.lockscreen.LockScreenPreferences
import com.sukanth.resonance.lockscreen.MediaSessionMonitor
import com.sukanth.resonance.lockscreen.PlayerVisualTheme
import com.sukanth.resonance.ui.lockscreen.LockScreenPlayerScreen
import com.sukanth.resonance.ui.theme.ResonanceTheme

class LockScreenActivity : ComponentActivity() {
    private val playerViewModel: ExternalPlayerViewModel by viewModels()
    private var userPresentReceiverRegistered = false
    private var previewMode by mutableStateOf(false)
    private var exitRequested by mutableStateOf(false)
    private var unlockAfterExit = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val finishAfterExit = Runnable {
        if (unlockAfterExit) {
            unlockAfterExit = false
            requestSystemUnlock()
        } else {
            dismissImmediately()
        }
    }
    private val finishIfUnlocked = Runnable {
        if (!previewMode && !isKeyguardLocked()) dismissImmediately()
    }

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) dismissImmediately()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewMode = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.decorView.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        setContent {
            val state by playerViewModel.state.collectAsStateWithLifecycle()
            var displayedState by remember { mutableStateOf(state) }

            LaunchedEffect(
                state,
                state.connected,
                state.notificationAccessGranted,
                previewMode,
            ) {
                if (
                    previewMode ||
                    state.isLockScreenEligible
                ) {
                    displayedState = state
                } else {
                    dismissAnimated()
                }
            }

            val visualTheme = remember { LockScreenPreferences.getVisualTheme(this@LockScreenActivity) }
            ResonanceTheme(darkTheme = true) {
                LockScreenPlayerScreen(
                    state = displayedState,
                    visualTheme = visualTheme,
                    isExiting = exitRequested,
                    onPlayPause = playerViewModel::togglePlayback,
                    onPrevious = playerViewModel::previous,
                    onNext = playerViewModel::next,
                    onPlaylistItemSelected = playerViewModel::playPlaylistEntry,
                    onOpenSourceApp = playerViewModel::openSourceApp,
                    onSeek = playerViewModel::seekTo,
                    onVolumeChange = playerViewModel::setVolume,
                    onLike = playerViewModel::toggleLike,
                    onShuffle = playerViewModel::toggleShuffle,
                    onUnlock = ::requestUnlock,
                    onClose = ::dismissAnimated,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!userPresentReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(userPresentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(userPresentReceiver, filter)
            }
            userPresentReceiverRegistered = true
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.refresh()
        mainHandler.removeCallbacks(finishIfUnlocked)
        mainHandler.post(finishIfUnlocked)
        mainHandler.postDelayed(finishIfUnlocked, 250L)
    }

    override fun onStop() {
        if (userPresentReceiverRegistered) {
            unregisterReceiver(userPresentReceiver)
            userPresentReceiverRegistered = false
        }
        super.onStop()
        mainHandler.postDelayed(finishIfUnlocked, 300L)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        previewMode = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        exitRequested = false
        unlockAfterExit = false
        mainHandler.removeCallbacks(finishAfterExit)
        mainHandler.post(finishIfUnlocked)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) mainHandler.postDelayed(finishIfUnlocked, 80L)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (playerViewModel.state.value.callActive) return super.onKeyDown(keyCode, event)
        val volumeDirection = volumeDirectionFor(keyCode)
        if (volumeDirection != null) {
            MediaSessionMonitor.optimisticVolumeStep(volumeDirection)
            // Return false to allow system audio service to adjust stream volume smoothly.
            // MediaSessionMonitor handles the resulting broadcast and updates state cleanly.
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (volumeDirectionFor(keyCode) != null) return false
        return super.onKeyUp(keyCode, event)
    }

    private fun volumeDirectionFor(keyCode: Int): Int? =
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            KeyEvent.KEYCODE_VOLUME_MUTE -> AudioManager.ADJUST_TOGGLE_MUTE
            else -> null
        }

    override fun onDestroy() {
        mainHandler.removeCallbacks(finishIfUnlocked)
        mainHandler.removeCallbacks(finishAfterExit)
        super.onDestroy()
    }

    private fun requestUnlock() {
        if (exitRequested || isFinishing) return
        unlockAfterExit = true
        exitRequested = true
        mainHandler.removeCallbacks(finishAfterExit)
        mainHandler.postDelayed(finishAfterExit, EXIT_ANIMATION_MS)
    }

    private fun requestSystemUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = dismissImmediately()
                    override fun onDismissCancelled() = restoreAfterUnlockFailure()
                    override fun onDismissError() = restoreAfterUnlockFailure()
                },
            )
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }

    private fun restoreAfterUnlockFailure() {
        mainHandler.post { exitRequested = false }
    }

    private fun isKeyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun dismissImmediately() {
        mainHandler.removeCallbacks(finishIfUnlocked)
        mainHandler.removeCallbacks(finishAfterExit)
        unlockAfterExit = false
        finishAndRemoveTask()
    }

    private fun dismissAnimated() {
        if (exitRequested || isFinishing) return
        unlockAfterExit = false
        exitRequested = true
        mainHandler.removeCallbacks(finishAfterExit)
        mainHandler.postDelayed(finishAfterExit, EXIT_ANIMATION_MS)
    }

    companion object {
        private const val EXTRA_PREVIEW = "com.sukanth.resonance.extra.PREVIEW"
        private const val EXIT_ANIMATION_MS = 300L

        fun createIntent(context: Context, preview: Boolean = false): Intent =
            Intent(context, LockScreenActivity::class.java)
                .putExtra(EXTRA_PREVIEW, preview)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                )
    }
}
