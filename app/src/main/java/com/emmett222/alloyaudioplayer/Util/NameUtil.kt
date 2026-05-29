package com.emmett222.alloyaudioplayer.Util

import android.graphics.Color
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.TreeMap

/**
 * A utilities class for commonly used functions that deal with names.
 *
 * @author Emmett Grebe
 * @version 5-29-2026
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
     * @return Int Color based off of the first 3 letters of the string.
     */
    fun getColorFromName(name: String, alphabetTree: TreeMap<Char, Int>) : Int {
        val char0 = if (name.isNotEmpty()) name[0].uppercaseChar() else 'A'
        val char1 = if (name.length > 1) name[1].uppercaseChar() else 'A'
        val char2 = if (name.length > 2) name[2].uppercaseChar() else 'A'

        val r = alphabetTree.getOrDefault(char0, 0)
        val g = alphabetTree.getOrDefault(char1, 0)
        val b = alphabetTree.getOrDefault(char2, 0)

        return Color.rgb(r, g, b)
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