package com.sukanth.resonance.lockscreen

import android.app.PendingIntent
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.MediaMetadata
import android.media.AudioManager
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val NO_ARTWORK_SIGNATURE = Long.MIN_VALUE

@Immutable
enum class PlaylistProvider {
    MEDIA_SESSION,
    POWERAMP_PROVIDER,
    MEDIA_BROWSER,
    IMPORTED,
}

@Immutable
data class PlaylistEntry(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val provider: PlaylistProvider = PlaylistProvider.MEDIA_SESSION,
    val mediaId: String? = null,
    val uri: String? = null,
)

@Immutable
data class ExternalMediaState(
    val notificationAccessGranted: Boolean = false,
    val connected: Boolean = false,
    val canShowOnLockScreen: Boolean = false,
    val packageName: String? = null,
    val sourceApp: String = "No media app",
    val title: String = "Nothing is playing",
    val artist: String = "Start music in Spotify, YouTube Music, or another player",
    val album: String = "",
    val artwork: Bitmap? = null,
    val artworkSignature: Long = NO_ARTWORK_SIGNATURE,
    val artworkPrimaryArgb: Int = 0xFF343B54.toInt(),
    val artworkSecondaryArgb: Int = 0xFF151923.toInt(),
    val playlist: List<PlaylistEntry> = emptyList(),
    val playlistProvider: PlaylistProvider? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 0f,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaybackActive: Boolean = false,
    val canPlayPause: Boolean = false,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val canSeek: Boolean = false,
    val canLike: Boolean = false,
    val likeAction: String? = null,
    val isLiked: Boolean = false,
    val shuffleAction: String? = null,
    val repeatAction: String? = null,
    val volume: Int = 0,
    val maxVolume: Int = 1,
    val callActive: Boolean = false,
    val error: String? = null,
) {
    val isLockScreenEligible: Boolean
        get() = notificationAccessGranted && connected && canShowOnLockScreen && !callActive
}

/**
 * Observes media sessions published by other apps and forwards transport commands to the
 * currently relevant session. Access is available only after the user enables our notification
 * listener in Android settings.
 */
object MediaSessionMonitor {
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(ExternalMediaState())
    val state: StateFlow<ExternalMediaState> = _state.asStateFlow()

    private lateinit var appContext: Context
    private lateinit var sessionManager: MediaSessionManager
    private lateinit var audioManager: AudioManager
    private lateinit var listenerComponent: ComponentName
    private var initialized = false
    private var sessionsListenerRegistered = false
    private var controller: MediaController? = null
    private var cachedArtworkSource: Bitmap? = null
    private var cachedArtworkGenerationId = 0
    private var cachedArtwork: Bitmap? = null
    private var cachedArtworkSignature = NO_ARTWORK_SIGNATURE
    private var cachedArtworkPrimaryArgb = DEFAULT_ARTWORK_PRIMARY
    private var cachedArtworkSecondaryArgb = DEFAULT_ARTWORK_SECONDARY
    private var cachedSourceLabel = "Media app"
    private var powerampPermissionRequested = false
    private var powerampProviderQueueLoaded = false
    private var mediaBrowserPlaylist: List<PlaylistEntry> = emptyList()
    private var mediaBrowserPackage: String? = null
    private var mediaBrowserRequestInFlight = false
    private var lastMediaBrowserRequestAt = 0L
    private var activeMediaNotificationPackages: Set<String> = emptySet()
    private val hideEligibility = Runnable {
        val current = _state.value
        val packageName = current.packageName
        if (
            current.connected &&
            packageName != null &&
            packageName !in activeMediaNotificationPackages
        ) {
            _state.value = current.copy(canShowOnLockScreen = false)
        }
    }
    private var playbackPublishScheduled = false
    private var pendingPlaybackState: PlaybackState? = null
    private var pendingVolumeTarget: Int? = null
    private var pendingVolumeUntilMs = 0L
    private var volumeBroadcastReceiverRegistered = false
    private val likedTrackKeys = mutableSetOf<String>()
    private val publishSettledMetadata = Runnable { publish() }
    private val publishConfirmedVolume = Runnable { publishVolumeOnly() }
    private val reconcileVolume = Runnable { publishVolumeOnly() }
    private val publishPlaybackFrame = Runnable {
        playbackPublishScheduled = false
        val pending = pendingPlaybackState
        pendingPlaybackState = null
        publishPlaybackOnly(pending)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            pendingPlaybackState = playbackState
            if (!playbackPublishScheduled) {
                playbackPublishScheduled = true
                handler.postDelayed(publishPlaybackFrame, PLAYBACK_FRAME_MS)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            scheduleMetadataPublish()
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
            val now = SystemClock.elapsedRealtime()
            if (pendingVolumeTarget == null || now >= pendingVolumeUntilMs) {
                publishVolumeOnly()
            }
        }

        override fun onSessionDestroyed() {
            handler.removeCallbacks(publishSettledMetadata)
            val destroyed = controller
            destroyed?.unregisterCallback(this)
            controller = null
            refreshSessions()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            selectController(controllers.orEmpty())
        }

    /**
     * BroadcastReceiver registered globally (with RECEIVER_EXPORTED on API 33+) so that
     * android.media.VOLUME_CHANGED_ACTION system broadcasts — which originate from system_server
     * UID 1000 — are not dropped on Android 13+ when RECEIVER_NOT_EXPORTED would block them.
     */
    private val volumeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_VOLUME_CHANGED) return
            val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
            // Accept STREAM_MUSIC explicitly, or accept untyped broadcasts from OEMs that omit
            // the extra (streamType == -1).
            if (streamType != AudioManager.STREAM_MUSIC && streamType != -1) return
            val broadcastVolume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
            // Many OEM builds (Samsung OneUI, Xiaomi HyperOS, etc.) omit EXTRA_VOLUME_STREAM_VALUE.
            // In that case query AudioManager directly rather than silently ignoring the event.
            val volume = if (broadcastVolume >= 0) broadcastVolume
                else audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            onSystemVolumeChanged(AudioManager.STREAM_MUSIC, volume)
        }
    }

    fun initialize(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
        audioManager = appContext.getSystemService(AudioManager::class.java)
        listenerComponent = ComponentName(appContext, MediaAccessService::class.java)
        initialized = true
        updateAccessState()
        // Seed real device volume into state immediately instead of leaving it at default 0.
        publishVolumeOnly()
        registerVolumeBroadcastReceiver()
    }

    fun hasNotificationAccess(): Boolean {
        check(initialized) { "MediaSessionMonitor must be initialized first" }
        return NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName)
    }

    fun connect() {
        if (!initialized) return
        val accessGranted = updateAccessState()
        if (!accessGranted) return

        if (!sessionsListenerRegistered) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(
                    sessionsChangedListener,
                    listenerComponent,
                    handler,
                )
                sessionsListenerRegistered = true
            } catch (_: SecurityException) {
                _state.value = _state.value.copy(
                    notificationAccessGranted = false,
                    connected = false,
                    error = "Notification access is required to control other media apps.",
                )
                return
            }
        }
        refreshSessions()
    }

    fun disconnect() {
        if (!initialized) return
        if (sessionsListenerRegistered) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            sessionsListenerRegistered = false
        }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        handler.removeCallbacks(publishSettledMetadata)
        handler.removeCallbacks(publishConfirmedVolume)
        handler.removeCallbacks(publishPlaybackFrame)
        handler.removeCallbacks(hideEligibility)
        handler.removeCallbacks(reconcileVolume)
        playbackPublishScheduled = false
        pendingPlaybackState = null
        pendingVolumeTarget = null
        pendingVolumeUntilMs = 0L
        resetArtworkCache()
        cachedSourceLabel = "Media app"
        powerampPermissionRequested = false
        powerampProviderQueueLoaded = false
        MediaBrowserPlaylistBridge.disconnect()
        mediaBrowserPlaylist = emptyList()
        mediaBrowserPackage = null
        mediaBrowserRequestInFlight = false
        lastMediaBrowserRequestAt = 0L
        _state.value = _state.value.copy(
            connected = false,
            canShowOnLockScreen = false,
            isPlaybackActive = false,
        )
    }

    fun refreshSessions() {
        if (!initialized || !hasNotificationAccess()) {
            updateAccessState()
            return
        }
        try {
            selectController(sessionManager.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
            _state.value = _state.value.copy(
                notificationAccessGranted = false,
                connected = false,
                error = "Android has not granted media notification access yet.",
            )
        }
    }

    fun onMediaNotificationPosted(packageName: String) {
        activeMediaNotificationPackages = activeMediaNotificationPackages + packageName
        handler.removeCallbacks(hideEligibility)
        if (packageName == _state.value.packageName) {
            _state.value = _state.value.copy(canShowOnLockScreen = true)
        }
    }

    fun onMediaNotificationRemoved(packageName: String) {
        activeMediaNotificationPackages = activeMediaNotificationPackages - packageName
        // Media apps often replace their notification during pause/play. Delay hiding until the
        // replacement settles so the lock-screen surface never flashes during a transport change.
        if (packageName == _state.value.packageName) scheduleEligibilityHide()
    }

    /**
     * NotificationListenerService's active snapshot is more reliable than individual removal
     * callbacks on OEM builds that strip MediaStyle extras from the removed notification.
     */
    fun onActiveMediaNotificationsChanged(activePackages: Set<String>) {
        activeMediaNotificationPackages = activePackages
        val currentPackage = _state.value.packageName ?: return
        if (currentPackage in activePackages) {
            handler.removeCallbacks(hideEligibility)
            if (_state.value.connected) {
                _state.value = _state.value.copy(canShowOnLockScreen = true)
            }
        } else if (_state.value.connected) {
            scheduleEligibilityHide()
        }
    }

    fun onCallStateChanged(active: Boolean) {
        val current = _state.value
        if (current.callActive == active) return
        _state.value = current.copy(callActive = active)
        if (!active) refreshSessions()
    }

    fun togglePlayPause() {
        controller?.let { activeController ->
            val state = activeController.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                activeController.transportControls.pause()
            } else {
                activeController.transportControls.play()
            }
        }
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    private fun trackKey(title: String?, artist: String?): String {
        val t = title.orEmpty().trim().lowercase()
        val a = artist.orEmpty().trim().lowercase()
        return if (t.isEmpty() && a.isEmpty()) "" else "$t|$a"
    }

    private fun isTrackLiked(metadata: MediaMetadata?, title: String?, artist: String?): Boolean {
        if (metadata?.getRating(MediaMetadata.METADATA_KEY_RATING)?.isPositive() == true) {
            return true
        }
        val key = trackKey(title, artist)
        return key.isNotEmpty() && likedTrackKeys.contains(key)
    }

    fun toggleLike() {
        val activeController = controller ?: return
        val current = _state.value
        val title = current.title
        val artist = current.artist
        val key = trackKey(title, artist)

        val currentlyLiked = isTrackLiked(activeController.metadata, title, artist)
        val targetLiked = !currentlyLiked

        if (key.isNotEmpty()) {
            if (targetLiked) {
                likedTrackKeys.add(key)
            } else {
                likedTrackKeys.remove(key)
            }
        }

        // Always update UI state immediately for snappy feedback.
        _state.value = current.copy(isLiked = targetLiked)

        // ── Only dispatch to the media app when LIKING ──────────────────────────────
        // Dispatching an un-like (targetLiked = false) causes music apps (Spotify, YouTube
        // Music, etc.) to interpret it as a "thumbs-down / dislike" and automatically skip
        // to the next song. We keep the un-like purely local: the heart un-fills in the UI
        // but the app is not told to dislike the track.
        if (!targetLiked) return

        val playback = activeController.playbackState
        if (playback != null && playback.actions.hasAny(PlaybackState.ACTION_SET_RATING)) {
            val newRating = when (activeController.ratingType) {
                Rating.RATING_HEART -> Rating.newHeartRating(true)
                Rating.RATING_THUMB_UP_DOWN -> Rating.newThumbRating(true)
                else -> Rating.newHeartRating(true)
            }
            if (newRating != null) {
                runCatching { activeController.transportControls.setRating(newRating) }
                return
            }
        }

        playback?.findLikeCustomAction()?.let { action ->
            runCatching { activeController.transportControls.sendCustomAction(action, null) }
        }
    }

    fun toggleShuffle() {
        val activeController = controller ?: return
        val action = activeController.playbackState?.findShuffleCustomAction() ?: return
        runCatching { activeController.transportControls.sendCustomAction(action, null) }
        handler.postDelayed({ publishPlaybackOnly(activeController.playbackState) }, 100L)
    }

    fun cycleRepeat() {
        val activeController = controller ?: return
        val action = activeController.playbackState?.findRepeatCustomAction() ?: return
        runCatching { activeController.transportControls.sendCustomAction(action, null) }
        handler.postDelayed({ publishPlaybackOnly(activeController.playbackState) }, 100L)
    }

    fun playPlaylistEntry(entry: PlaylistEntry) {
        val activeController = controller ?: return
        when (entry.provider) {
            PlaylistProvider.MEDIA_SESSION -> {
                entry.id.toLongOrNull()?.takeIf { it >= 0L }?.let {
                    activeController.transportControls.skipToQueueItem(it)
                }
            }
            PlaylistProvider.POWERAMP_PROVIDER -> {
                entry.id.toLongOrNull()?.takeIf { it >= 0L }?.let {
                    PowerampPlaylistBridge.playQueueItem(appContext, it)
                }
            }
            PlaylistProvider.MEDIA_BROWSER -> {
                val mediaId = entry.mediaId ?: entry.id
                runCatching {
                    activeController.transportControls.playFromMediaId(mediaId, null)
                }
            }
            PlaylistProvider.IMPORTED -> {
                // Never launch an arbitrary shared URI while the real keyguard is locked.
                val keyguardManager = appContext.getSystemService(KeyguardManager::class.java)
                if (keyguardManager?.isKeyguardLocked == true) return
                val sharedUri = entry.uri?.let(Uri::parse)
                if (sharedUri != null && sharedUri.scheme != null) {
                    runCatching {
                        appContext.startActivity(
                            Intent(Intent.ACTION_VIEW, sharedUri)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                } else {
                    runCatching {
                        activeController.transportControls.playFromSearch(entry.title, null)
                    }
                }
            }
        }
    }

    /** Backwards-compatible transport helper for callers that only have a framework queue id. */
    fun playQueueItem(queueId: Long) {
        if (queueId >= 0L) {
            controller?.transportControls?.skipToQueueItem(queueId)
        }
    }

    fun onImportedPlaylistChanged() {
        if (initialized && controller != null) publish()
    }

    fun seekTo(positionMs: Long) {
        controller?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setVolume(volume: Int) {
        if (!initialized || _state.value.callActive) return
        val maximum = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        val target = volume.coerceIn(0, maximum)
        val applied = runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }.isSuccess
        if (!applied) return

        // AudioManager may report the previous value for a short interval on OEM builds. Keep
        // the slider at the requested media volume until the stream broadcasts its confirmation.
        markPendingVolume(target, maximum)
        handler.removeCallbacks(publishConfirmedVolume)
        handler.postDelayed(publishConfirmedVolume, VOLUME_CONFIRM_MS)
    }

    fun adjustVolume(direction: Int) {
        if (direction !in VALID_VOLUME_DIRECTIONS) return
        val current = _state.value
        if (!initialized || current.callActive) return
        val maximum = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        val applied = runCatching {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }.isSuccess
        if (applied) {
            val delta = when (direction) {
                AudioManager.ADJUST_RAISE -> 1
                AudioManager.ADJUST_LOWER -> -1
                else -> 0
            }
            val target = (current.volume + delta).coerceIn(0, maximum)
            markPendingVolume(target, maximum)
        }
        handler.removeCallbacks(publishConfirmedVolume)
        handler.postDelayed(publishConfirmedVolume, VOLUME_CONFIRM_MS)
    }

    /** Moves only the rendered slider; the unconsumed hardware event changes Android's stream. */
    fun optimisticVolumeStep(direction: Int) {
        if (!initialized || _state.value.callActive || direction !in VALID_VOLUME_DIRECTIONS) return
        val current = _state.value
        val maximum = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        val target = when (direction) {
            AudioManager.ADJUST_RAISE -> current.volume + 1
            AudioManager.ADJUST_LOWER -> current.volume - 1
            else -> current.volume
        }.coerceIn(0, maximum)
        markPendingVolume(target, maximum)
        handler.removeCallbacks(publishConfirmedVolume)
        handler.postDelayed(publishConfirmedVolume, VOLUME_CONFIRM_MS)
    }

    /** Reconciles hardware/OEM volume changes observed outside our key-event callback. */
    fun refreshVolume() {
        handler.removeCallbacks(reconcileVolume)
        handler.post(reconcileVolume)
    }

    /** Publishes the value already confirmed by Android without a second AudioService round trip. */
    fun onSystemVolumeChanged(streamType: Int, volume: Int) {
        if (!initialized) return
        // Accept STREAM_MUSIC explicitly; also accept -1/untyped from OEMs that omit the extra.
        if (streamType != AudioManager.STREAM_MUSIC && streamType != -1) return
        val current = _state.value
        val maximum = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        // If the broadcast volume payload is absent (< 0) fall back to a direct AudioManager read
        // rather than silently ignoring the event, which left the slider stuck on many OEM builds.
        val effectiveVolume = if (volume >= 0) volume
            else audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val confirmedVolume = resolvedVolume(effectiveVolume.coerceIn(0, maximum), maximum)
        if (current.volume != confirmedVolume || current.maxVolume != maximum) {
            _state.value = current.copy(volume = confirmedVolume, maxVolume = maximum)
        }
    }

    fun openSourceApp() {
        try {
            controller?.sessionActivity?.send()
        } catch (_: PendingIntent.CanceledException) {
            _state.value = _state.value.copy(error = "The media app could not be opened.")
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun updateAccessState(): Boolean {
        val granted = hasNotificationAccess()
        _state.value = _state.value.copy(
            notificationAccessGranted = granted,
            error = if (granted) null else _state.value.error,
        )
        return granted
    }

    /** Updates the tiny volume slice without decoding artwork or rebuilding media metadata. */
    private fun publishVolumeOnly() {
        if (!initialized) return
        val current = _state.value
        val actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        val volume = resolvedVolume(actualVolume.coerceIn(0, maxVolume), maxVolume)
        if (volume != current.volume || maxVolume != current.maxVolume) {
            _state.value = current.copy(volume = volume, maxVolume = maxVolume)
        }
    }

    private fun markPendingVolume(target: Int, maximum: Int) {
        pendingVolumeTarget = target.coerceIn(0, maximum)
        pendingVolumeUntilMs = SystemClock.elapsedRealtime() + VOLUME_GRACE_MS
        val current = _state.value
        if (current.volume != target || current.maxVolume != maximum) {
            _state.value = current.copy(volume = target, maxVolume = maximum)
        }
    }

    private fun resolvedVolume(actual: Int, maximum: Int): Int {
        val pending = pendingVolumeTarget ?: return actual
        val now = SystemClock.elapsedRealtime()
        return if (now < pendingVolumeUntilMs) {
            // Keep the pending target for the entire duration of the grace window.
            // Do NOT clear the shield prematurely even if an intermediate broadcast matches,
            // because trailing out-of-order broadcasts would otherwise overwrite the state with stale values.
            pending.coerceIn(0, maximum)
        } else {
            pendingVolumeTarget = null
            pendingVolumeUntilMs = 0L
            actual
        }
    }

    private fun selectController(controllers: List<MediaController>) {
        val candidates = controllers.filter {
            it.packageName != appContext.packageName &&
                it.packageName in activeMediaNotificationPackages
        }
        // Keep the current source while its session is still valid. Notification/session
        // callbacks arrive out of order; immediately re-sorting here makes the app-menu
        // label appear to change while a track is still playing.
        val currentCandidate = candidates.firstOrNull { it.sessionToken == controller?.sessionToken }
        val playingCandidate = candidates.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: candidates.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_BUFFERING
        }
        val selected = currentCandidate?.takeIf {
            it.playbackState?.state !in setOf(
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_ERROR,
            ) || playingCandidate == null
        } ?: playingCandidate ?: candidates.firstOrNull()

        if (controller?.sessionToken == selected?.sessionToken) {
            publishPlaybackOnly(selected?.playbackState)
            scheduleMetadataPublish()
            return
        }

        val previous = controller
        previous?.unregisterCallback(controllerCallback)
        controller = selected
        if (selected == null) {
            publishExpiredState()
            return
        }

        resetArtworkCache()
        powerampProviderQueueLoaded = false
        mediaBrowserPlaylist = emptyList()
        mediaBrowserPackage = null
        mediaBrowserRequestInFlight = false
        lastMediaBrowserRequestAt = 0L
        MediaBrowserPlaylistBridge.disconnect()
        if (selected.packageName == PowerampPlaylistBridge.PACKAGE && !powerampPermissionRequested) {
            powerampPermissionRequested = true
            PowerampPlaylistBridge.requestDataAccess(appContext)
        }
        cachedSourceLabel = selected.packageName
            .let(::applicationLabel)
            .safeText(64, "Media app")
        selected.registerCallback(controllerCallback, handler)
        publish()
    }

    private fun scheduleMetadataPublish() {
        handler.removeCallbacks(publishSettledMetadata)
        handler.postDelayed(publishSettledMetadata, METADATA_SETTLE_MS)
    }

    private fun requestMediaBrowserPlaylist(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        if (
            mediaBrowserPackage == packageName &&
            (mediaBrowserRequestInFlight || now - lastMediaBrowserRequestAt < MEDIA_BROWSER_REFRESH_MS)
        ) return

        mediaBrowserPackage = packageName
        mediaBrowserRequestInFlight = true
        lastMediaBrowserRequestAt = now
        MediaBrowserPlaylistBridge.load(appContext, packageName) { resultPackage, entries ->
            handler.post {
                if (controller?.packageName != resultPackage) return@post
                mediaBrowserRequestInFlight = false
                mediaBrowserPlaylist = entries.take(MAX_QUEUE_ITEMS)
                publish()
            }
        }
    }

    private fun scheduleEligibilityHide() {
        handler.removeCallbacks(hideEligibility)
        handler.postDelayed(hideEligibility, ELIGIBILITY_SETTLE_MS)
    }

    /** Keeps frequent position and transport callbacks away from artwork and palette work. */
    private fun publishPlaybackOnly(playback: PlaybackState?) {
        val activeController = controller ?: return
        val current = _state.value
        if (!current.connected) {
            publish()
            return
        }

        val playbackState = playback?.state ?: PlaybackState.STATE_NONE
        val isPlaybackActive = playbackState in ACTIVE_PLAYBACK_STATES
        val actions = playback?.actions ?: 0L
        val safePlaybackSpeed = playback?.playbackSpeed
            ?.takeIf { it.isFinite() }
            ?.coerceIn(-MAX_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
            ?: 0f
        val next = current.copy(
            positionMs = playback?.estimatedPosition(current.durationMs) ?: current.positionMs,
            playbackSpeed = safePlaybackSpeed,
            isPlaying = playbackState == PlaybackState.STATE_PLAYING,
            isBuffering = playbackState.isBufferingState(),
            isPlaybackActive = isPlaybackActive,
            canPlayPause = actions.hasAny(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE,
            ),
            canGoPrevious = actions.hasAny(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            canGoNext = actions.hasAny(PlaybackState.ACTION_SKIP_TO_NEXT),
            canSeek = actions.hasAny(PlaybackState.ACTION_SEEK_TO) && current.durationMs > 0L,
            canLike = actions.hasAny(PlaybackState.ACTION_SET_RATING) ||
                playback?.findLikeCustomAction() != null,
            likeAction = playback?.findLikeCustomAction(),
            isLiked = isTrackLiked(activeController.metadata, current.title, current.artist),
            shuffleAction = playback?.findShuffleCustomAction(),
            repeatAction = playback?.findRepeatCustomAction(),
        )
        if (next != current) _state.value = next
    }

    private fun publish() {
        val activeController = controller
        if (activeController == null) {
            if (!_state.value.connected) publishExpiredState()
            return
        }

        val metadata = activeController.metadata
        val playback = activeController.playbackState
        val actions = playback?.actions ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.coerceIn(0L, MAX_MEDIA_DURATION_MS) ?: 0L
        val playbackState = playback?.state ?: PlaybackState.STATE_NONE
        val isPlaying = playbackState == PlaybackState.STATE_PLAYING
        val isPlaybackActive = playbackState in ACTIVE_PLAYBACK_STATES
        val safePlaybackSpeed = playback?.playbackSpeed
            ?.takeIf { it.isFinite() }
            ?.coerceIn(-MAX_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
            ?: 0f
        val estimatedPosition = playback?.estimatedPosition(duration) ?: 0L
        val artwork = sanitizedArtwork(metadata?.artwork())
        // Some Poweramp releases expose a queue that changes while the session is being
        // published. Read it defensively so a transient provider failure cannot crash Resonance.
        val mediaSessionPlaylist = runCatching {
            activeController.queue.orEmpty().take(MAX_QUEUE_ITEMS).mapIndexed { index, item ->
                val description = item.description
                PlaylistEntry(
                    id = item.queueId.toString().ifBlank { index.toString() },
                    title = description.title?.toString().safeText(180, "Unknown title"),
                    artist = description.subtitle?.toString().safeText(180, "Unknown artist"),
                    album = description.description?.toString().safeText(180, ""),
                    provider = PlaylistProvider.MEDIA_SESSION,
                )
            }
        }.getOrDefault(emptyList())
        val powerampPlaylist = if (
            activeController.packageName == PowerampPlaylistBridge.PACKAGE &&
                mediaSessionPlaylist.isEmpty()
        ) {
            runCatching { PowerampPlaylistBridge.readQueue(appContext) }
                .getOrDefault(emptyList())
                .take(MAX_QUEUE_ITEMS)
                .also { powerampProviderQueueLoaded = it.isNotEmpty() }
        } else {
            powerampProviderQueueLoaded = false
            emptyList()
        }
        val sourcePackage = activeController.packageName
        val importedPlaylist = ImportedPlaylistStore.readForPackage(appContext, sourcePackage)
            .take(MAX_QUEUE_ITEMS)
        val browserPlaylist = mediaBrowserPlaylist
            .takeIf { mediaBrowserPackage == sourcePackage }
            .orEmpty()
        val playlist = when {
            mediaSessionPlaylist.isNotEmpty() -> mediaSessionPlaylist
            powerampPlaylist.isNotEmpty() -> powerampPlaylist
            browserPlaylist.isNotEmpty() -> browserPlaylist
            else -> importedPlaylist
        }.take(MAX_QUEUE_ITEMS)
        val playlistProvider = when {
            mediaSessionPlaylist.isNotEmpty() -> PlaylistProvider.MEDIA_SESSION
            powerampPlaylist.isNotEmpty() -> PlaylistProvider.POWERAMP_PROVIDER
            browserPlaylist.isNotEmpty() -> PlaylistProvider.MEDIA_BROWSER
            importedPlaylist.isNotEmpty() -> PlaylistProvider.IMPORTED
            else -> null
        }
        val maximumVolume = audioManager
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            .coerceAtLeast(1)
        val renderedVolume = resolvedVolume(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                .coerceIn(0, maximumVolume),
            maximumVolume,
        )

        if (mediaSessionPlaylist.isEmpty() && powerampPlaylist.isEmpty()) {
            requestMediaBrowserPlaylist(sourcePackage)
        }

        val currentTitle = (
            metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ).safeText(180, "Unknown title")
        val currentArtist = (
            metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ).safeText(180, "Unknown artist")

        _state.value = ExternalMediaState(
            notificationAccessGranted = true,
            connected = true,
            // Keep the existing surface through transient metadata/playback updates. A newly
            // observed source notification can surface it; a debounced removal hides it later.
            canShowOnLockScreen = if (activeController.packageName in activeMediaNotificationPackages) {
                true
            } else {
                _state.value.canShowOnLockScreen
            },
            packageName = activeController.packageName,
            sourceApp = cachedSourceLabel,
            title = currentTitle,
            artist = currentArtist,
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).safeText(180, ""),
            artwork = artwork,
            artworkSignature = cachedArtworkSignature,
            artworkPrimaryArgb = cachedArtworkPrimaryArgb,
            artworkSecondaryArgb = cachedArtworkSecondaryArgb,
            playlist = playlist,
            playlistProvider = playlistProvider,
            positionMs = estimatedPosition.coerceIn(
                0L,
                duration.takeIf { it > 0L } ?: Long.MAX_VALUE,
            ),
            durationMs = duration,
            playbackSpeed = safePlaybackSpeed,
            isPlaying = isPlaying,
            isBuffering = playbackState.isBufferingState(),
            isPlaybackActive = isPlaybackActive,
            canPlayPause = actions.hasAny(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE,
            ),
            canGoPrevious = actions.hasAny(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            canGoNext = actions.hasAny(PlaybackState.ACTION_SKIP_TO_NEXT),
            canSeek = actions.hasAny(PlaybackState.ACTION_SEEK_TO) && duration > 0L,
            canLike = actions.hasAny(PlaybackState.ACTION_SET_RATING) ||
                playback?.findLikeCustomAction() != null,
            likeAction = playback?.findLikeCustomAction(),
            isLiked = isTrackLiked(metadata, currentTitle, currentArtist),
            shuffleAction = playback?.findShuffleCustomAction(),
            repeatAction = playback?.findRepeatCustomAction(),
            volume = renderedVolume,
            maxVolume = maximumVolume,
            callActive = _state.value.callActive,
        )
    }

    private fun PlaybackState.estimatedPosition(durationMs: Long): Long {
        val basePosition = position.coerceAtLeast(0L)
        if (state !in POSITION_ADVANCING_STATES) {
            return basePosition.coerceIn(0L, durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)
        }
        val elapsed = (SystemClock.elapsedRealtime() - lastPositionUpdateTime).coerceAtLeast(0L)
        val safeSpeed = playbackSpeed
            .takeIf { it.isFinite() }
            ?.coerceIn(-MAX_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
            ?: 0f
        return (basePosition + (elapsed * safeSpeed).toLong()).coerceIn(
            0L,
            durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE,
        )
    }

    private fun Int.isBufferingState(): Boolean =
        this == PlaybackState.STATE_BUFFERING || this == PlaybackState.STATE_CONNECTING

    private fun PlaybackState.findLikeCustomAction(): String? = findCustomAction(
        keywords = listOf("like", "thumb", "heart", "favorite"),
        excludedKeywords = listOf("shuffle", "random", "repeat", "next", "previous", "skip"),
    )

    private fun PlaybackState.findShuffleCustomAction(): String? = findCustomAction(
        keywords = listOf("shuffle", "random"),
        excludedKeywords = listOf("like", "thumb", "heart", "favorite", "repeat"),
    )

    private fun PlaybackState.findRepeatCustomAction(): String? = findCustomAction(
        keywords = listOf("repeat", "loop"),
        excludedKeywords = listOf("like", "thumb", "heart", "favorite", "shuffle", "random"),
    )

    private fun PlaybackState.findCustomAction(
        keywords: List<String>,
        excludedKeywords: List<String> = emptyList(),
    ): String? =
        customActions
            .firstOrNull { action ->
                val searchable = "${action.action} ${action.name}".lowercase()
                keywords.any(searchable::contains) &&
                    excludedKeywords.none(searchable::contains)
            }
            ?.action

    private fun Rating?.isPositive(): Boolean = this?.isRated == true &&
        (hasHeart() || isThumbUp)

    @Suppress("DEPRECATION")
    private fun applicationLabel(packageName: String): String = runCatching {
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        val label = appContext.packageManager.getApplicationLabel(info).toString().trim()
        if (label.isNotBlank() && label != packageName) label else packageName.toReadableAppName()
    }.getOrElse { packageName.toReadableAppName() }

    private fun String.toReadableAppName(): String = substringAfterLast('.')
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('-', ' ')
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
        .ifBlank { "Media app" }

    private fun Long.hasAny(action: Long): Boolean = this and action != 0L

    @Suppress("DEPRECATION")
    private fun MediaMetadata.artwork(): Bitmap? =
        getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    private fun sanitizedArtwork(source: Bitmap?): Bitmap? {
        if (source == null) {
            // The metadata callback is already delayed briefly to let players publish their
            // artwork. Once the settled metadata still has no cover, clear every cached artwork
            // value so a coverless song can never inherit the previous song's image or palette.
            resetArtworkCache()
            return null
        }
        if (
            source === cachedArtworkSource &&
            source.generationId == cachedArtworkGenerationId
        ) {
            return cachedArtwork
        }
        val result = runCatching {
            val largestSide = maxOf(source.width, source.height)
            val resized = if (largestSide <= MAX_ARTWORK_SIDE_PX) {
                source
            } else {
                val scale = MAX_ARTWORK_SIDE_PX.toFloat() / largestSide.toFloat()
                source.scale(
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                )
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                resized.config == Bitmap.Config.HARDWARE
            ) {
                resized.copy(Bitmap.Config.ARGB_8888, false)?.also {
                    if (resized !== source) resized.recycle()
                } ?: resized
            } else {
                resized
            }
        }.getOrDefault(source)
        cachedArtworkSource = source
        cachedArtworkGenerationId = source.generationId
        cachedArtwork = result
        cachedArtworkSignature = result.sampledSignature()
        val palette = extractArtworkPalette(result)
        cachedArtworkPrimaryArgb = palette.first
        cachedArtworkSecondaryArgb = palette.second
        return result
    }

    private fun resetArtworkCache() {
        cachedArtworkSource = null
        cachedArtworkGenerationId = 0
        cachedArtwork = null
        cachedArtworkSignature = NO_ARTWORK_SIGNATURE
        cachedArtworkPrimaryArgb = DEFAULT_ARTWORK_PRIMARY
        cachedArtworkSecondaryArgb = DEFAULT_ARTWORK_SECONDARY
    }

    /** Samples at most 24x24 pixels and quantizes them; no resized bitmap or palette dependency. */
    private fun extractArtworkPalette(bitmap: Bitmap): Pair<Int, Int> = runCatching {
        val weights = IntArray(COLOR_BUCKET_COUNT)
        val redSums = IntArray(COLOR_BUCKET_COUNT)
        val greenSums = IntArray(COLOR_BUCKET_COUNT)
        val blueSums = IntArray(COLOR_BUCKET_COUNT)
        val sampleStepX = (bitmap.width / COLOR_SAMPLE_SIDE).coerceAtLeast(1)
        val sampleStepY = (bitmap.height / COLOR_SAMPLE_SIDE).coerceAtLeast(1)

        var y = sampleStepY / 2
        while (y < bitmap.height) {
            var x = sampleStepX / 2
            while (x < bitmap.width) {
                val pixel = bitmap[x, y]
                if (AndroidColor.alpha(pixel) >= 160) {
                    val red = AndroidColor.red(pixel)
                    val green = AndroidColor.green(pixel)
                    val blue = AndroidColor.blue(pixel)
                    val chroma = maxOf(red, green, blue) - minOf(red, green, blue)
                    val weight = 2 + chroma / 24
                    val bucket = ((red shr 5) shl 6) or
                        ((green shr 5) shl 3) or
                        (blue shr 5)
                    weights[bucket] += weight
                    redSums[bucket] += red * weight
                    greenSums[bucket] += green * weight
                    blueSums[bucket] += blue * weight
                }
                x += sampleStepX
            }
            y += sampleStepY
        }

        val primaryBucket = weights.indices.maxByOrNull(weights::get)
            ?.takeIf { weights[it] > 0 }
            ?: return@runCatching DEFAULT_ARTWORK_PRIMARY to DEFAULT_ARTWORK_SECONDARY
        val primary = bucketColor(primaryBucket, weights, redSums, greenSums, blueSums)

        var secondaryBucket = primaryBucket
        var secondaryScore = Long.MIN_VALUE
        for (bucket in weights.indices) {
            if (weights[bucket] == 0 || bucket == primaryBucket) continue
            val candidate = bucketColor(bucket, weights, redSums, greenSums, blueSums)
            val redDistance = AndroidColor.red(candidate) - AndroidColor.red(primary)
            val greenDistance = AndroidColor.green(candidate) - AndroidColor.green(primary)
            val blueDistance = AndroidColor.blue(candidate) - AndroidColor.blue(primary)
            val distance = redDistance * redDistance +
                greenDistance * greenDistance +
                blueDistance * blueDistance
            val score = weights[bucket].toLong() * distance.coerceAtLeast(1)
            if (score > secondaryScore) {
                secondaryScore = score
                secondaryBucket = bucket
            }
        }
        val secondary = bucketColor(secondaryBucket, weights, redSums, greenSums, blueSums)
        softenForDarkSurface(primary) to softenForDarkSurface(secondary)
    }.getOrDefault(DEFAULT_ARTWORK_PRIMARY to DEFAULT_ARTWORK_SECONDARY)

    /** Stable, low-cost artwork identity. Equivalent republished bitmaps keep the same key. */
    private fun Bitmap.sampledSignature(): Long = runCatching {
        var hash = ARTWORK_HASH_SEED
        val stepX = (width / ARTWORK_HASH_SIDE).coerceAtLeast(1)
        val stepY = (height / ARTWORK_HASH_SIDE).coerceAtLeast(1)
        var y = stepY / 2
        while (y < height) {
            var x = stepX / 2
            while (x < width) {
                hash = (hash xor this[x, y].toLong()) * ARTWORK_HASH_PRIME
                x += stepX
            }
            y += stepY
        }
        hash xor (width.toLong() shl 32) xor height.toLong()
    }.getOrDefault(0L)

    private fun bucketColor(
        bucket: Int,
        weights: IntArray,
        redSums: IntArray,
        greenSums: IntArray,
        blueSums: IntArray,
    ): Int {
        val weight = weights[bucket].coerceAtLeast(1)
        return AndroidColor.rgb(
            redSums[bucket] / weight,
            greenSums[bucket] / weight,
            blueSums[bucket] / weight,
        )
    }

    private fun softenForDarkSurface(color: Int): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceIn(0f, 0.82f)
        hsv[2] = hsv[2].coerceIn(0.24f, 0.62f)
        return AndroidColor.HSVToColor(hsv)
    }

    private fun String?.safeText(maxLength: Int, fallback: String): String {
        val cleaned = this
            ?.filterNot { Character.isISOControl(it) }
            ?.trim()
            ?.take(maxLength)
        return cleaned?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun publishExpiredState() {
        val current = _state.value
        _state.value = current.copy(
            connected = false,
            canShowOnLockScreen = false,
            playlist = emptyList(),
            playlistProvider = null,
            isPlaying = false,
            isBuffering = false,
            isPlaybackActive = false,
            playbackSpeed = 0f,
            canPlayPause = false,
            canGoPrevious = false,
            canGoNext = false,
            canSeek = false,
            canLike = false,
            likeAction = null,
            isLiked = false,
            shuffleAction = null,
            repeatAction = null,
        )
    }

    private val ACTIVE_PLAYBACK_STATES = setOf(
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
    )

    private val POSITION_ADVANCING_STATES = setOf(
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
    )

    private val VALID_VOLUME_DIRECTIONS = setOf(
        AudioManager.ADJUST_RAISE,
        AudioManager.ADJUST_LOWER,
        AudioManager.ADJUST_TOGGLE_MUTE,
    )

    private const val MAX_PLAYBACK_SPEED = 8f
    private const val PLAYBACK_FRAME_MS = 32L
    private const val METADATA_SETTLE_MS = 40L
    private const val ELIGIBILITY_SETTLE_MS = 850L
    private const val VOLUME_CONFIRM_MS = 80L
    // Grace window after a local volume change during which incoming broadcasts / AudioInfo
    // callbacks are suppressed. Must be long enough to outlast 1-2 system broadcast round-trips
    // but short enough that a subsequent hardware key-press is not silently ignored.
    private const val VOLUME_GRACE_MS = 450L
    private const val MEDIA_BROWSER_REFRESH_MS = 5_000L
    private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    private const val MAX_ARTWORK_SIDE_PX = 640
    private const val MAX_MEDIA_DURATION_MS = 7L * 24L * 60L * 60L * 1_000L
    private const val MAX_QUEUE_ITEMS = 100
    private const val COLOR_SAMPLE_SIDE = 24
    private const val COLOR_BUCKET_COUNT = 512
    private const val ARTWORK_HASH_SIDE = 8
    private const val ARTWORK_HASH_SEED = -3_750_763_034_362_895_579L
    private const val ARTWORK_HASH_PRIME = 1_099_511_628_211L
    private const val DEFAULT_ARTWORK_PRIMARY: Int = -13_354_156
    private const val DEFAULT_ARTWORK_SECONDARY: Int = -15_394_525

    /**
     * Registers a BroadcastReceiver that listens for system volume change events. This must
     * use RECEIVER_EXPORTED on API 33+ because the broadcast originates from system_server
     * (UID 1000), which is outside the app process. RECEIVER_NOT_EXPORTED would silently
     * block every system-originated volume broadcast on Android 13 and newer.
     */
    private fun registerVolumeBroadcastReceiver() {
        if (volumeBroadcastReceiverRegistered) return
        val filter = IntentFilter(ACTION_VOLUME_CHANGED)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(volumeBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(volumeBroadcastReceiver, filter)
            }
            volumeBroadcastReceiverRegistered = true
        }
    }

    private fun unregisterVolumeBroadcastReceiver() {
        if (!volumeBroadcastReceiverRegistered) return
        runCatching { appContext.unregisterReceiver(volumeBroadcastReceiver) }
        volumeBroadcastReceiverRegistered = false
    }
}
