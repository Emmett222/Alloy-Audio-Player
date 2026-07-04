package com.emmett222.alloyaudioplayer.Util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import java.io.File

/**
 * Utility object for help with files.
 *
 * @author Emmett Grebe
 * @version 7-4-2026
 */
object FileUtil {
    /**
     * Gets the duration of an audio file. Must be an audio file. Uses MediaStore to get the length
     * of the audio file.
     * This uses MediaStore instead of MediaMetaDataRetriever because this can run better in the
     * background and does not use up the UI thread.
     *
     * @param context The context of the audio file.
     * @param filePath The filepath to the audio file.
     * @return A long of the audio file's length.
     */
    fun getDurationFromFile(context: Context, filePath: String): Long {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI // URI for primary storage.
        val projection = arrayOf(MediaStore.Audio.Media.DURATION) // Get the duration.

        // MediaStore.Audio.Media.DATA is a constant that evaluated to the database column that
        // stores the absolute file path of an audio file.
        // '= ?' is a placeholder. It tells the rest that there will be more coming and that it is
        // not empty. This is called a Parameterized Query. Parameterized Queries prevent SQL
        // injection and parsing errors. Audio files commonly have strange characters in their names
        // which could break parts of the system.
        val selection = "${MediaStore.Audio.Media.DATA} = ?"

        val selectionArgs = arrayOf(filePath)

        // Query is a filter to find given parameters.
        // .use is a Kotlin safety valve. If anything happens, like returning or crashing, the
        // database cursor is automatically closed. This prevents memory leaks.
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            // Move the cursor to the first matching row. If nothing there, return false and skip.
            if (cursor.moveToFirst()) {
                // Get the column index for the duration of the audio file.
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                // Get the duration of the audio file from the previously obtained index.
                return cursor.getLong(durationIndex)
            }
        }
        return 0L // Return 0 if there was an issue along the way.
    }

    /**
     * Gets the name of the artist from a file.
     *
     * @param file The audio file to get the author from.
     * @return A string containing the author of the given audio file.
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