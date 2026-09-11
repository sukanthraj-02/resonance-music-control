package com.sukanth.resonance.lockscreen

import android.content.Context
import androidx.core.content.edit

object LockScreenPreferences {
    private const val FILE_NAME = "lock_screen_preferences"
    private const val KEY_ENABLED = "lock_screen_enabled"
    private const val KEY_AUTOMATIC_PLAYER = "automatic_player_enabled"
    private const val KEY_FAB_X_DP = "fab_x_dp"
    private const val KEY_FAB_Y_DP = "fab_y_dp"
    private const val KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED =
        "accessibility_disclosure_accepted"

    const val ACTION_AUTOMATIC_PLAYER_CHANGED =
        "com.sukanth.resonance.action.AUTOMATIC_PLAYER_CHANGED"

    fun isEnabled(context: Context): Boolean = context
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, enabled)
        }
    }

    fun isAutomaticPlayerEnabled(context: Context): Boolean = context
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_AUTOMATIC_PLAYER, false)

    fun setAutomaticPlayerEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_AUTOMATIC_PLAYER, enabled)
        }
        context.sendBroadcast(
            android.content.Intent(ACTION_AUTOMATIC_PLAYER_CHANGED)
                .setPackage(context.packageName),
        )
    }

    fun fabPositionDp(context: Context): Pair<Int, Int> {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return preferences.getInt(KEY_FAB_X_DP, DEFAULT_FAB_X_DP) to
            preferences.getInt(KEY_FAB_Y_DP, DEFAULT_FAB_Y_DP)
    }

    fun setFabPositionDp(context: Context, x: Int, y: Int) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_FAB_X_DP, x)
            putInt(KEY_FAB_Y_DP, y)
        }
    }

    fun setAccessibilityDisclosureAccepted(context: Context, accepted: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED, accepted)
        }
    }

    fun getVisualTheme(context: Context): PlayerVisualTheme {
        val stored = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VISUAL_THEME, null)
        return PlayerVisualTheme.fromStoredValue(stored)
    }

    fun setVisualTheme(context: Context, theme: PlayerVisualTheme) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_VISUAL_THEME, theme.name)
        }
    }

    private const val KEY_VISUAL_THEME = "lock_screen_visual_theme"
    private const val DEFAULT_FAB_X_DP = 28
    private const val DEFAULT_FAB_Y_DP = 176
}
