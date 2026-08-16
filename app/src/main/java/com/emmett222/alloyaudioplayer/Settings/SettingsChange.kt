package com.emmett222.alloyaudioplayer.Settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

/**
 * Object that can set and get settings.
 *
 * @author Emmett Grebe
 * @version 8-16-2026
 */
object SettingsChange {
    private const val PREFS_NAME = "AlloyPlayerPrefs"

    const val KEY_COLOR_TYPE_1 = "color_type_1"
    const val KEY_COLOR_TYPE_2 = "color_type_2"
    const val KEY_COLOR_TYPE_3 = "color_type_3"
    const val KEY_ANIM_TYPE = "animation_type"
    const val KEY_SHORT_MODE = "shorten_type"
    const val KEY_SORT_TYPE = "sort_type"
    const val KEY_SORT_DIR = "sort_dir"
    const val KEY_VIS_TYPE = "visualizer_type"
    const val KEY_REPEAT_MODE = "repeat_mode"
    const val KEY_SHUFFLE_MODE = "shuffle_mode"

    /**
     * Gets the preferences. Gets them in MODE_PRIVATE so only Alloy can read and write to them.
     *
     * @param context The Context of the app.
     * @return The preferences object.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- COLOR SETTINGS ---
    fun saveColor1(context: Context, color: Int) {
        getPrefs(context).edit { putInt(KEY_COLOR_TYPE_1, color) }
    }
    fun getColor1(context: Context): Int {
        // The second parameter (#00FF00) is the default fallback if the ledger is empty
        return getPrefs(context).getInt(KEY_COLOR_TYPE_1, Color.parseColor("#00FF00"))
    }
    fun saveColor2(context: Context, color: Int) {
        getPrefs(context).edit { putInt(KEY_COLOR_TYPE_2, color) }
    }
    fun getColor2(context: Context): Int {
        // The second parameter (#0d380c) is the default fallback if the ledger is empty
        return getPrefs(context).getInt(KEY_COLOR_TYPE_2, Color.parseColor("#0d380c"))
    }
    fun saveColor3(context: Context, color: Int) {
        getPrefs(context).edit { putInt(KEY_COLOR_TYPE_3, color) }
    }
    fun getColor3(context: Context): Int {
        // The second parameter (#0d380c) is the default fallback if the ledger is empty
        return getPrefs(context).getInt(KEY_COLOR_TYPE_3, Color.parseColor("#00FF00"))
    }

    // --- ANIMATION SETTINGS ---
    fun saveAnimType(context: Context, animType: Int) {
        getPrefs(context).edit { putInt(KEY_ANIM_TYPE, animType) }
    }
    fun getAnimType(context: Context): Int {
        // The second parameter (0) is the default fallback if the ledger is empty
        return getPrefs(context).getInt(KEY_ANIM_TYPE, 0)
    }

    // --- SHORTEN SETTINGS ---
    fun saveShortMode(context: Context, isRepeat: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SHORT_MODE, isRepeat) }
    }
    fun getShortMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHORT_MODE, false)
    }

    // --- SORT BY SETTINGS ---
    fun saveSortType(context: Context, type: Int) {
        getPrefs(context).edit { putInt(KEY_SORT_TYPE, 0) }
    }
    fun getSortType(context: Context): Int {
        return getPrefs(context).getInt(KEY_SORT_TYPE, 0)
    }
    // --- SORT DIRECTION SETTINGS ---
    fun saveSortDir(context: Context, type: Int) {
        getPrefs(context).edit { putInt(KEY_SORT_DIR, 0) }
    }
    fun getSortDir(context: Context): Int {
        return getPrefs(context).getInt(KEY_SORT_DIR, 0)
    }

    // --- REPEAT SETTINGS ---
    fun saveRepeatMode(context: Context, isRepeat: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REPEAT_MODE, isRepeat) }
    }
    fun getRepeatMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REPEAT_MODE, false)
    }

    // --- SHUFFLE SETTINGS ---
    fun saveShufMode(context: Context, isRepeat: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SHUFFLE_MODE, isRepeat) }
    }
    fun getShufMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHUFFLE_MODE, false)
    }

    // --- VISUALIZER SETTINGS ---
    fun saveVisType(context: Context, visType: Int) {
        getPrefs(context).edit { putInt(KEY_VIS_TYPE, visType) }
    }
    fun getVisType(context: Context): Int {
        // The second parameter (5) is the default fallback if the ledger is empty
        return getPrefs(context).getInt(KEY_VIS_TYPE, 5)
    }
}