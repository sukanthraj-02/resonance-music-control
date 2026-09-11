package com.sukanth.resonance.lockscreen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.browse.MediaBrowser
import android.media.browse.MediaBrowser.MediaItem
import android.service.media.MediaBrowserService
import android.os.Handler
import android.os.Looper

/**
 * Bounded discovery of playable children exposed by a player's MediaBrowserService. This is a
 * discovery fallback only: a player may expose no browser service or may expose only its library.
 */
object MediaBrowserPlaylistBridge {
    private const val MAX_ITEMS = 100
    private const val MAX_BROWSABLE_NODES = 12
    private const val MAX_DEPTH = 2
    private const val DISCOVERY_TIMEOUT_MS = 1_400L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var browser: MediaBrowser? = null
    private var generation = 0

    fun load(
        context: Context,
        packageName: String,
        onResult: (packageName: String, entries: List<PlaylistEntry>) -> Unit,
    ) {
        disconnect()
        val service = findService(context, packageName)
        if (service == null) {
            onResult(packageName, emptyList())
            return
        }

        val requestGeneration = ++generation
        val collector = Collector(
            packageName = packageName,
            requestGeneration = requestGeneration,
            onResult = onResult,
        )
        val connectionCallback = object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                if (requestGeneration != generation) return
                val rootId = browser?.root
                if (rootId.isNullOrBlank()) {
                    collector.finish()
                } else {
                    collector.subscribe(rootId, 0)
                }
            }

            override fun onConnectionFailed() = collector.finish()

            override fun onConnectionSuspended() = collector.finish()
        }
        browser = runCatching {
            MediaBrowser(
                context.applicationContext,
                service,
                connectionCallback,
                null,
            )
        }.getOrNull()
        if (browser == null) {
            collector.finish()
        } else {
            collector.startTimeout()
            browser?.connect()
        }
    }

    fun disconnect() {
        generation++
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { browser?.disconnect() }
        browser = null
    }

    private fun findService(context: Context, packageName: String): ComponentName? = runCatching {
        val intent = Intent(MediaBrowserService.SERVICE_INTERFACE)
        context.packageManager
            .queryIntentServices(intent, PackageManager.MATCH_ALL)
            .firstOrNull { it.serviceInfo.packageName == packageName }
            ?.serviceInfo
            ?.let { ComponentName(it.packageName, it.name) }
    }.getOrNull()

    private class Collector(
        private val packageName: String,
        private val requestGeneration: Int,
        private val onResult: (packageName: String, entries: List<PlaylistEntry>) -> Unit,
    ) {
        private val entries = LinkedHashMap<String, PlaylistEntry>()
        private val subscribed = mutableSetOf<String>()
        private val depthByParent = mutableMapOf<String, Int>()
        private var browsableCount = 0
        private var finished = false
        private val callback = object : MediaBrowser.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaItem>) {
                collect(parentId, children)
            }

            override fun onError(parentId: String) = finish()
        }

        fun startTimeout() {
            mainHandler.postDelayed({ finish() }, DISCOVERY_TIMEOUT_MS)
        }

        fun subscribe(parentId: String, depth: Int) {
            if (finished || requestGeneration != generation || !subscribed.add(parentId)) return
            depthByParent[parentId] = depth
            runCatching { browser?.subscribe(parentId, callback) }
                .onFailure { finish() }
        }

        private fun collect(parentId: String, children: List<MediaItem>) {
            if (finished || requestGeneration != generation) return
            val depth = depthByParent[parentId] ?: 0
            children.forEach { item ->
                val description = item.description
                if (item.isPlayable && entries.size < MAX_ITEMS) {
                    val mediaId = description.mediaId ?: return@forEach
                    entries[mediaId] = PlaylistEntry(
                        id = "browser:${mediaId.hashCode()}",
                        title = description.title?.toString()?.trim().orEmpty()
                            .ifBlank { "Unknown title" },
                        artist = description.subtitle?.toString()?.trim().orEmpty(),
                        album = description.description?.toString()?.trim().orEmpty(),
                        provider = PlaylistProvider.MEDIA_BROWSER,
                        mediaId = mediaId,
                    )
                }
                if (
                    item.isBrowsable &&
                    browsableCount < MAX_BROWSABLE_NODES &&
                    depth < MAX_DEPTH
                ) {
                    browsableCount++
                    subscribe(description.mediaId ?: return@forEach, depth + 1)
                }
            }
            if (entries.size >= MAX_ITEMS) finish()
        }

        fun finish() {
            if (finished || requestGeneration != generation) return
            finished = true
            runCatching { browser?.disconnect() }
            browser = null
            onResult(packageName, entries.values.toList())
        }
    }
}
