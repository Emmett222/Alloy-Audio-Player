package com.emmett222.alloyaudioplayer.Settings

import android.content.Context
import android.graphics.Color
import java.io.File
import androidx.core.graphics.toColorInt

/**
 * Imports and exports settings JSONs.
 *
 * @author Emmett Grebe
 * @version 8-17-2026
 */
object SettingsIO {
    /**
     * Imports an Alloy Settings JSON and changes settings based off of its contents.
     *
     * @param jsonString A JSON file containing every setting and its value.
     * @param context Context needed to get the settings.
     */
    fun importSettings(jsonString: String, context: Context) {
        val pairs = jsonString.split(',')
        val splitPairs: HashMap<String, String> = HashMap()
        pairs.forEach {
            val colonSplit = it.split(':')
            splitPairs[colonSplit[0]] = colonSplit[1]
        }

        SettingsChange.saveColor1(
            context,
            splitPairs[SettingsChange.KEY_COLOR_TYPE_1]?.toIntOrNull()
                ?: "#00FF00".toColorInt()
        )
        SettingsChange.saveColor2(
            context,
            splitPairs[SettingsChange.KEY_COLOR_TYPE_2]?.toIntOrNull()
                ?: "#0d380c".toColorInt()
        )
        SettingsChange.saveColor3(
            context,
            splitPairs[SettingsChange.KEY_COLOR_TYPE_3]?.toIntOrNull()
                ?: "#00FF00".toColorInt()
        )

        SettingsChange.saveDefaultFolder(context, splitPairs[SettingsChange.KEY_DFOLDER_TYPE] ?: "")

        SettingsChange.saveAnimType(
            context,
            splitPairs[SettingsChange.KEY_ANIM_TYPE]?.toIntOrNull() ?: 0
        )
        SettingsChange.saveSortType(
            context,
            splitPairs[SettingsChange.KEY_SORT_TYPE]?.toIntOrNull() ?: 0
        )
        SettingsChange.saveSortDir(
            context,
            splitPairs[SettingsChange.KEY_SORT_DIR]?.toIntOrNull() ?: 1
        )
        SettingsChange.saveVisType(
            context,
            splitPairs[SettingsChange.KEY_VIS_TYPE]?.toIntOrNull() ?: 5
        )

        // toBooleanStrictOrNull() is incredibly safe. It only accepts "true" or "false".
        SettingsChange.saveShortMode(
            context,
            splitPairs[SettingsChange.KEY_SHORT_MODE]?.toBooleanStrictOrNull() ?: false
        )
        SettingsChange.saveRepeatMode(
            context,
            splitPairs[SettingsChange.KEY_REPEAT_MODE]?.toBooleanStrictOrNull() ?: false
        )
        SettingsChange.saveShufMode(
            context,
            splitPairs[SettingsChange.KEY_SHUFFLE_MODE]?.toBooleanStrictOrNull() ?: false
        )
        SettingsChange.saveDisconnectMode(
            context,
            splitPairs[SettingsChange.KEY_DISCONNECT_MODE]?.toBooleanStrictOrNull() ?: true
        )
    }

    /**
     * Exports current user settings as a JSON.
     *
     * @param context Context needed to get the settings.
     * @return The JSON created as a String.
     * @throws java.io.IOException
     */
    fun exportSettings(context: Context): String {
        var jsonString = "{"
        context.getSharedPreferences(
            SettingsChange.PREFS_NAME,
            Context.MODE_PRIVATE
        ).all.forEach { string, any ->
            jsonString += "\"$string\":\"$any.toString()\","
        }
        jsonString += "}"

        return jsonString
    }

    /**
     * Takes a key and a value string and converts it into a JSON key value pair.
     *
     * @param key The key to be used.
     * @param value The value to be used.
     * @return A JSON key value pair. Example: " "Key":"Value" "
     */
    private fun jsonIfy(key: String, value: String): String {
        return "\"$key\":\"$value\","
    }
}