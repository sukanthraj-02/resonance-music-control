package com.sukanth.resonance

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.sukanth.resonance.lockscreen.ImportedPlaylistStore
import com.sukanth.resonance.lockscreen.MediaSessionMonitor

/** Receives user-selected songs or playlist links shared from another music app. */
class ShareImportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imported = ImportedPlaylistStore.importFromIntent(this, intent)
        MediaSessionMonitor.initialize(this)
        MediaSessionMonitor.onImportedPlaylistChanged()
        Toast.makeText(
            this,
            if (imported > 0) "$imported item(s) imported" else "No songs or playlist link found",
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }
}
