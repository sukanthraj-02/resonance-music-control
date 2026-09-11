package com.sukanth.resonance.lockscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, local fallback for songs or playlist links shared from a player. It deliberately stores
 * only user-shared labels/links; it never reads another app's private database or media files.
 */
object ImportedPlaylistStore {
    private const val PREFERENCES = "shared_playlist_imports"
    private const val RECORDS = "records"
    private const val MAX_ENTRIES = 100

    fun importFromIntent(context: Context, intent: Intent): Int {
        val sourcePackage = inferSourcePackage(intent)
        val entries = parseEntries(context, intent)
        if (entries.isEmpty()) return 0

        val existing = readRecords(context)
            .filterNot { it.sourcePackage == sourcePackage }
            .toMutableList()
        existing += ImportedRecord(sourcePackage, entries.take(MAX_ENTRIES))
        writeRecords(context, existing.takeLast(8))
        return entries.size
    }

    fun readForPackage(context: Context, packageName: String?): List<PlaylistEntry> {
        val records = readRecords(context)
        val exact = records.lastOrNull { it.sourcePackage == packageName }
        val generic = records.lastOrNull { it.sourcePackage == null }
        return (exact ?: generic)?.entries.orEmpty()
    }

    private fun parseEntries(
        context: Context,
        intent: Intent,
    ): List<PlaylistEntry> {
        val sharedUris = buildList {
            when (val stream = intent.extras?.get(Intent.EXTRA_STREAM)) {
                is Uri -> add(stream)
                is ArrayList<*> -> stream.filterIsInstance<Uri>().forEach(::add)
            }
        }.distinct()
        val sharedTexts = buildList {
            when (val shared = intent.extras?.get(Intent.EXTRA_TEXT)) {
                is CharSequence -> add(shared.toString())
                is ArrayList<*> -> shared.filterIsInstance<CharSequence>().forEach { add(it.toString()) }
            }
        }.flatMap { it.lines() }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        if (sharedTexts.isEmpty() && sharedUris.isEmpty()) return emptyList()
        val urls = sharedTexts.filter { it.isUrl() }
        val shareTitle = intent.getStringExtra(Intent.EXTRA_TITLE).orEmpty().trim()
        val values = if (urls.isNotEmpty()) {
            urls.map { url ->
                val title = sharedTexts
                    .takeWhile { it != url }
                    .lastOrNull { !it.isUrl() }
                    ?.takeIf { it.isNotBlank() }
                    ?: shareTitle
                    .takeIf { it.isNotBlank() }
                    ?: url.toUriLabel()
                ImportedValue(title = title, artist = "Shared link", uri = url)
            }
        } else {
            sharedTexts.take(MAX_ENTRIES).map { line ->
                val pieces = line.split(Regex("\\s+[—–-]\\s+"), limit = 2)
                ImportedValue(
                    title = pieces.first().trim(),
                    artist = pieces.getOrNull(1).orEmpty().trim(),
                    uri = null,
                )
            }
        }
        val streamValues = sharedUris.map { uri ->
            ImportedValue(
                title = queryDisplayName(context, uri).orEmpty().ifBlank { "Shared song" },
                artist = "Shared audio",
                uri = uri.toString(),
            )
        }
        return (values + streamValues).take(MAX_ENTRIES).mapIndexed { index, value ->
            PlaylistEntry(
                id = "imported-${System.currentTimeMillis()}-$index",
                title = value.title.take(180).ifBlank { "Shared song" },
                artist = value.artist.take(180),
                provider = PlaylistProvider.IMPORTED,
                uri = value.uri,
            )
        }
    }

    private fun inferSourcePackage(intent: Intent): String? {
        val referrer = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
            ?.let(Uri::parse)
            ?.host
            ?.takeIf { it.contains('.') }
        if (referrer != null && referrer != "android") return referrer

        val text = intent.extras?.get(Intent.EXTRA_TEXT)?.toString().orEmpty().lowercase()
        return when {
            "spotify.com" in text || "spotify:" in text -> "com.spotify.music"
            "poweramp" in text -> PowerampPlaylistBridge.PACKAGE
            "music.youtube.com" in text || "youtu.be" in text ->
                "com.google.android.apps.youtube.music"
            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private fun readRecords(context: Context): List<ImportedRecord> = runCatching {
        val json = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(RECORDS, null) ?: return@runCatching emptyList()
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val record = array.optJSONObject(index) ?: continue
                val entries = record.optJSONArray("entries") ?: continue
                add(
                    ImportedRecord(
                        sourcePackage = record.optString("sourcePackage").takeIf { it.isNotBlank() },
                        entries = buildList {
                            for (entryIndex in 0 until entries.length()) {
                                val item = entries.optJSONObject(entryIndex) ?: continue
                                add(
                                    PlaylistEntry(
                                        id = item.optString("id"),
                                        title = item.optString("title", "Shared song"),
                                        artist = item.optString("artist"),
                                        album = item.optString("album"),
                                        provider = PlaylistProvider.IMPORTED,
                                        uri = item.optString("uri").takeIf { it.isNotBlank() },
                                    ),
                                )
                            }
                        },
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun writeRecords(context: Context, records: List<ImportedRecord>) {
        val json = JSONArray()
        records.forEach { record ->
            val entries = JSONArray()
            record.entries.forEach { entry ->
                entries.put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("title", entry.title)
                        .put("artist", entry.artist)
                        .put("album", entry.album)
                        .put("uri", entry.uri.orEmpty()),
                )
            }
            json.put(
                JSONObject()
                    .put("sourcePackage", record.sourcePackage.orEmpty())
                    .put("entries", entries),
            )
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(RECORDS, json.toString())
            .apply()
    }

    private data class ImportedRecord(
        val sourcePackage: String?,
        val entries: List<PlaylistEntry>,
    )

    private data class ImportedValue(
        val title: String,
        val artist: String,
        val uri: String?,
    )

    private fun String.isUrl(): Boolean = startsWith("http://") || startsWith("https://") || startsWith("spotify:")

    private fun String.toUriLabel(): String = runCatching {
        Uri.parse(this).host?.removePrefix("www.") ?: this
    }.getOrDefault(this)
}
