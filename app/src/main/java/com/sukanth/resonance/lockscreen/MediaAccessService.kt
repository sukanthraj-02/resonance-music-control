package com.sukanth.resonance.lockscreen

import android.app.Notification
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

/**
 * System-bound media data source. It does no rendering, polling, wake locking, or activity starts.
 */
class MediaAccessService : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val knownMediaNotificationKeys = mutableSetOf<String>()
    private val syncActiveNotifications = Runnable(::publishActiveNotificationSnapshot)

    override fun onCreate() {
        super.onCreate()
        // Remove the legacy Resonance entry notification from older builds.
        NotificationManagerCompat.from(this).cancel(LEGACY_PLAYER_NOTIFICATION_ID)
        MediaSessionMonitor.initialize(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaSessionMonitor.connect()
        scheduleSnapshot(delayMs = 0L)
    }

    override fun onListenerDisconnected() {
        mainHandler.removeCallbacks(syncActiveNotifications)
        knownMediaNotificationKeys.clear()
        MediaSessionMonitor.disconnect()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.isCallNotification() || isCallAudioModeActive()) {
            MediaSessionMonitor.onCallStateChanged(true)
            scheduleSnapshot(delayMs = NOTIFICATION_POST_SETTLE_MS)
            return
        }
        if (!notification.isMediaNotification()) return
        knownMediaNotificationKeys += notification.key
        MediaSessionMonitor.onMediaNotificationPosted(notification.packageName)
        MediaSessionMonitor.refreshSessions()
        scheduleSnapshot(delayMs = NOTIFICATION_POST_SETTLE_MS)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.isCallNotification()) {
            scheduleSnapshot(delayMs = NOTIFICATION_REMOVE_SETTLE_MS)
        }
        val wasKnownMediaNotification = knownMediaNotificationKeys.remove(notification.key)
        val belongsToCurrentPlayer =
            notification.packageName == MediaSessionMonitor.state.value.packageName
        if (wasKnownMediaNotification || notification.isMediaNotification()) {
            MediaSessionMonitor.onMediaNotificationRemoved(notification.packageName)
        }
        if (wasKnownMediaNotification || belongsToCurrentPlayer) {
            MediaSessionMonitor.refreshSessions()
            // OEM notification managers can still include the removed entry during its callback.
            scheduleSnapshot(delayMs = NOTIFICATION_REMOVE_SETTLE_MS)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(syncActiveNotifications)
        knownMediaNotificationKeys.clear()
        super.onDestroy()
    }

    private fun scheduleSnapshot(delayMs: Long) {
        mainHandler.removeCallbacks(syncActiveNotifications)
        mainHandler.postDelayed(syncActiveNotifications, delayMs)
    }

    private fun publishActiveNotificationSnapshot() {
        val notifications = runCatching { activeNotifications?.toList().orEmpty() }
            .getOrElse { return }
        val mediaNotifications = notifications.filter { it.isMediaNotification() }
        MediaSessionMonitor.onCallStateChanged(
            notifications.any { it.isCallNotification() } || isCallAudioModeActive(),
        )
        knownMediaNotificationKeys.clear()
        knownMediaNotificationKeys += mediaNotifications.map { it.key }
        MediaSessionMonitor.onActiveMediaNotificationsChanged(
            mediaNotifications.mapTo(mutableSetOf()) { it.packageName },
        )
        MediaSessionMonitor.refreshSessions()
    }

    private fun StatusBarNotification.isMediaNotification(): Boolean =
        !isCallNotification() &&
            (
                notification.category == Notification.CATEGORY_TRANSPORT ||
                    notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                )

    private fun StatusBarNotification.isCallNotification(): Boolean {
        val category = notification.category
        if (category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_MISSED_CALL) return true
        if (notification.extras.getBoolean("android.isCallNotification", false)) return true
        val template = notification.extras.getString(Notification.EXTRA_TEMPLATE)
        if (template?.contains("call", ignoreCase = true) == true) return true
        val actions = notification.actions.orEmpty()
        if (actions.any { action ->
            val title = action.title?.toString() ?: ""
            title.equals("Decline", ignoreCase = true) ||
                title.equals("Answer", ignoreCase = true) ||
                title.equals("Reject", ignoreCase = true) ||
                title.equals("Accept", ignoreCase = true)
        }) return true
        val pkg = packageName.lowercase()
        if (pkg.contains("dialer") || pkg.contains("incallui") || pkg.contains("telecom") || pkg.contains("phone")) {
            if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0 || notification.fullScreenIntent != null) {
                return true
            }
        }
        return false
    }

    private fun isCallAudioModeActive(): Boolean = runCatching {
        val audioManager = getSystemService(AudioManager::class.java)
        val telephonyManager = getSystemService(android.telephony.TelephonyManager::class.java)
        val isTelephonyRingingOrInCall = runCatching {
            @Suppress("DEPRECATION")
            telephonyManager?.callState != android.telephony.TelephonyManager.CALL_STATE_IDLE
        }.getOrDefault(false)

        isTelephonyRingingOrInCall || when (audioManager?.mode) {
            AudioManager.MODE_RINGTONE,
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            -> true
            else -> false
        }
    }.getOrDefault(false)

    private companion object {
        const val LEGACY_PLAYER_NOTIFICATION_ID = 4201
        const val NOTIFICATION_POST_SETTLE_MS = 12L
        const val NOTIFICATION_REMOVE_SETTLE_MS = 180L
    }
}
