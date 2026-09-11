package com.sukanth.resonance.lockscreen

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Optional Poweramp bridge. It never reads files directly; Poweramp controls provider access. */
object PowerampPlaylistBridge {
    const val PACKAGE = "com.maxmpz.audioplayer"
    private const val DATA_AUTHORITY = "com.maxmpz.audioplayer.data"
    private const val ASK_FOR_DATA_PERMISSION =
        "com.maxmpz.audioplayer.ACTION_ASK_FOR_DATA_PERMISSION"

    fun requestDataAccess(context: Context) {
        runCatching {
            context.sendBroadcast(
                Intent(ASK_FOR_DATA_PERMISSION)
                    .setPackage(PACKAGE)
                    .putExtra("package", context.packageName),
            )
        }
    }

    fun playQueueItem(context: Context, queueId: Long) {
        runCatching {
            context.sendBroadcast(
                Intent("com.maxmpz.audioplayer.API_COMMAND")
                    .setPackage(PACKAGE)
                    .putExtra("cmd", 20) // Poweramp OPEN_TO_PLAY
                    .putExtra("data", "content://$DATA_AUTHORITY/queue/$queueId")
                    .putExtra("pak", context.packageName),
            )
        }
    }

    fun readQueue(context: Context): List<PlaylistEntry> = runCatching {
        val uri = Uri.parse("content://$DATA_AUTHORITY/queue")
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val title = cursor.indexOf(
                "title", "track_title", "song_title", "name", "display_name", "track", "song",
            )
            val artist = cursor.indexOf(
                "artist", "artist_name", "track_artist", "performer", "author",
            )
            val album = cursor.indexOf("album", "album_name", "track_album", "release")
            val path = cursor.indexOf("path", "file", "file_path", "filename", "file_name", "data", "uri")
            val id = cursor.indexOf("_id", "id", "queue_id", "track_id")
            buildList {
                while (cursor.moveToNext() && size < 200) {
                    val pathText = cursor.textAt(path).orEmpty()
                    val titleText = cursor.textAt(title).orEmpty().ifBlank {
                        pathText.substringAfterLast('/').substringBeforeLast('.', pathText)
                    }.ifBlank {
                        cursor.firstUsefulText(setOf(title, artist, album, path, id))
                    }.ifBlank { "Unknown title" }
                    add(
                        PlaylistEntry(
                            id = cursor.textAt(id)?.toLongOrNull()?.toString() ?: "poweramp-$size",
                            title = titleText,
                            artist = cursor.textAt(artist).orEmpty(),
                            album = cursor.textAt(album).orEmpty(),
                            provider = PlaylistProvider.POWERAMP_PROVIDER,
                        ),
                    )
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    private fun android.database.Cursor.indexOf(vararg names: String): Int =
        names.firstNotNullOfOrNull { name ->
            getColumnNames().indexOfFirst { it.equals(name, ignoreCase = true) }
                .takeIf { it >= 0 }
        } ?: -1

    private fun android.database.Cursor.textAt(index: Int): String? =
        index.takeIf { it >= 0 && !isNull(it) }?.let { runCatching { getString(it) }.getOrNull() }

    private fun android.database.Cursor.firstUsefulText(excluded: Set<Int>): String =
        (0 until columnCount)
            .asSequence()
            .filterNot(excluded::contains)
            .mapNotNull { textAt(it)?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
            .orEmpty()
}
