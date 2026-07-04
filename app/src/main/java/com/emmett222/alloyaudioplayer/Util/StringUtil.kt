package com.emmett222.alloyaudioplayer.Util

/**
 * A utility object for commonly used functions dealing with strings.
 *
 * @author Emmett Grebe
 * @version 7-4-2026
 */
object StringUtil {
    /**
     * Format milliseconds to minutes and seconds.
     * @param m Time in milliseconds.
     * @return String of the time in the format of x:xx.
     */
    fun formatMinutesAndSeconds(m: Int): String {
        val minutes = (m / 1000) / 60
        val seconds = (m / 1000) % 60

        // Formats to x:xx.
        return String.format("%d:%02d", minutes, seconds)
    }
}