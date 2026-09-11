package com.sukanth.resonance.lockscreen

import android.app.Application
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel

class ExternalPlayerViewModel(application: Application) : AndroidViewModel(application) {
    val state = MediaSessionMonitor.state

    init {
        MediaSessionMonitor.initialize(application)
        MediaSessionMonitor.connect()
    }

    fun refresh() {
        MediaSessionMonitor.connect()
        MediaSessionMonitor.refreshSessions()
    }

    fun hasNotificationAccess(): Boolean = MediaSessionMonitor.hasNotificationAccess()

    fun hasUnrestrictedBatteryAccess(): Boolean {
        val application = getApplication<Application>()
        return application.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(application.packageName)
    }

    fun hasAccessibilityAccess(): Boolean {
        val application = getApplication<Application>()
        return application.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                info.packageName == application.packageName &&
                    info.name == ResonanceAccessibilityService::class.java.name
            }
    }

    fun togglePlayback() = MediaSessionMonitor.togglePlayPause()
    fun previous() = MediaSessionMonitor.previous()
    fun next() = MediaSessionMonitor.next()
    fun toggleLike() = MediaSessionMonitor.toggleLike()
    fun toggleShuffle() = MediaSessionMonitor.toggleShuffle()
    fun cycleRepeat() = MediaSessionMonitor.cycleRepeat()
    fun playPlaylistEntry(entry: PlaylistEntry) = MediaSessionMonitor.playPlaylistEntry(entry)
    fun seekTo(positionMs: Long) = MediaSessionMonitor.seekTo(positionMs)
    fun setVolume(volume: Int) = MediaSessionMonitor.setVolume(volume)
    fun adjustVolume(direction: Int) = MediaSessionMonitor.adjustVolume(direction)
    fun openSourceApp() = MediaSessionMonitor.openSourceApp()
    fun clearError() = MediaSessionMonitor.clearError()
}
