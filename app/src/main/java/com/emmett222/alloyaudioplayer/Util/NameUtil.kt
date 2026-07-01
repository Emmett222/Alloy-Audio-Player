package com.emmett222.alloyaudioplayer.Util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import java.io.File
import java.util.TreeMap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * A utilities class for commonly used functions that deal with names.
 *
 * @author Emmett Grebe
 * @version 7-1-2026
 */
object NameUtil {
    /**
     * Generates a TreeMap with:
     * - Keys: Each letter of the alphabet.
     * - Entries: 0-255 spread out amongst the letters evenly.
     *
     * @return TreeMap with aforementioned characteristics.
     */
    fun generateAlphabetTable(): TreeMap<Char, Int> {
        val table = TreeMap<Char, Int>()
        val alphabet = ('A'..'Z').toList()

        for (i in alphabet.indices) {
            val value = (i * 255) / 25
            table[alphabet[i]] = value
        }
        return table
    }

    /**
     * Uses the first 3 letters of a string to get a unique color. If string has less than 3
     * letters, then only remaining letters are used.
     *
     * @param name String to use.
     * @param alphabetTree TreeMap to use. This can be made from generateAlphabetTable(). Needs:
     * - Keys: Each letter of the alphabet.
     * - Entries: 0-255 spread out amongst the letters evenly.
     * @param gradient If the color will have a gradient or not.
     * @return Drawable based off of the first 3 letters of the string.
     */
    fun getColorFromName(name: String, alphabetTree: TreeMap<Char, Int>, gradient: Boolean) : Drawable {
        val char0 = if (name.isNotEmpty()) name[0].uppercaseChar() else 'A'
        val char1 = if (name.length > 1) name[1].uppercaseChar() else 'A'
        val char2 = if (name.length > 2) name[2].uppercaseChar() else 'A'

        val r = alphabetTree.getOrDefault(char0, 0)
        val g = alphabetTree.getOrDefault(char1, 0)
        val b = alphabetTree.getOrDefault(char2, 0)

        val baseColor = Color.rgb(r, g, b)
        if (!gradient) return baseColor.toDrawable()

        val dimAmount = 50
        val dimR = (Color.red(baseColor) - dimAmount).coerceIn(0, 255)
        val dimG = (Color.green(baseColor) - dimAmount).coerceIn(0, 255)
        val dimB = (Color.blue(baseColor) - dimAmount).coerceIn(0, 255)

        val secondaryColor = Color.rgb(dimR, dimG, dimB)
        val gradient: GradientDrawable = GradientDrawable()
        gradient.orientation = GradientDrawable.Orientation.BL_TR
        gradient.colors = intArrayOf(baseColor, secondaryColor)

        return gradient
    }

    /**
     * Uses the first 3 letters of a string to get a unique color. If string has less than 3
     * letters, then only remaining letters are used.
     *
     * @param name String to use.
     * @param alphabetTree TreeMap to use. This can be made from generateAlphabetTable(). Needs:
     * - Keys: Each letter of the alphabet.
     * - Entries: 0-255 spread out amongst the letters evenly.
     * @return Int Color based off of the first 3 letters of the string.
     */
    fun getColorFromName(name: String, alphabetTree: TreeMap<Char, Int>) : Int {
        val char0 = if (name.isNotEmpty()) name[0].uppercaseChar() else 'A'
        val char1 = if (name.length > 1) name[1].uppercaseChar() else 'A'
        val char2 = if (name.length > 2) name[2].uppercaseChar() else 'A'

        val r = alphabetTree.getOrDefault(char0, 0)
        val g = alphabetTree.getOrDefault(char1, 0)
        val b = alphabetTree.getOrDefault(char2, 0)

        val baseColor = Color.rgb(r, g, b)
        return baseColor
    }

    /**
     * Removes descriptors from the end of a file name. Descriptors like parenthesis or file type.
     * For example, "Song (H264-128KBIT_AAC).mp3" gets turned into "Song"
     *
     * @param name String to use.
     * @return Name with descriptors removed. If there were no descriptors, the original name given
     * is returned.
     */
    fun removeDescriptors(name: String) : String {
        val lastParenIndex = name.lastIndexOf('(')
        val lastPeriodIndex = name.lastIndexOf('.')
        if (lastParenIndex == -1 && lastPeriodIndex == -1) {
            return name
        }
        if (lastParenIndex == -1) {
            return name.substring(0, lastPeriodIndex)
        }
        return name.substring(0, lastParenIndex)
    }

    /**
     * Gets the name of the artist from a file.
     */
    fun getArtistFromFile(file: File) : String {
        val retriever = MediaMetadataRetriever()

        return try {
            // Sets the data source to the file path
            retriever.setDataSource(file.absolutePath)

            // Extracts the metadata field for ARTIST
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist" // Returns default if null

        } catch (e: Exception) {
            "Unknown Artist" // Handles errors (e.g., file not found or unsupported format)
        } finally {
            // Release the retriever to prevent memory leaks
            retriever.release()
        }
    }
}