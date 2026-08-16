package com.emmett222.alloyaudioplayer.Util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.emmett222.alloyaudioplayer.Settings.SettingsChange
import com.emmett222.alloyaudioplayer.Settings.SortDirection
import com.emmett222.alloyaudioplayer.Settings.SortType
import java.io.File

/**
 * Utility object for help with files.
 *
 * @author Emmett Grebe
 * @version 8-16-2026
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
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                // Move the cursor to the first matching row. If nothing there, return false and skip.
                if (cursor.moveToFirst()) {
                    // Get the column index for the duration of the audio file.
                    val durationIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    // Get the duration of the audio file from the previously obtained index.
                    return cursor.getLong(durationIndex)
                }
            }
        return 0L // Return 0 if there was an issue along the way.
    }

    /**
     * Determines if a file is an audio file or not by checking its extension.
     * Extensions include: mp3, m4a, opus, aac, aif, aiff, cda, flac, off, wav.
     *
     * @param file The file to check.
     * @return True if audio file, false if not.
     */
    fun isAudioFile(file: File): Boolean {
        val extensions =
            arrayOf("mp3", "m4a", "opus", "aac", "aif", "aiff", "cda", "flac", "ogg", "wav")
        return extensions.contains(file.extension)
    }

    /**
     * Filters files based on user settings.
     *
     * @param files An array of audio files.
     * @param context The context of the application.
     */
    fun filterFiles(files: Array<File>?, context: Context): Array<File> {
        // First remove all non audio files.
        val filteredFiles = files?.filter { file -> isAudioFile(file) }
        val folders = files?.filter { file -> file.isDirectory }

        val sortType = SettingsChange.getSortType(context)

        var sortedFolders: Array<File> = folders?.toTypedArray() ?: emptyArray()
        var sortedFiles: Array<File> = filteredFiles?.toTypedArray() ?: emptyArray()

        // Then sort based on user sort settings.
        when (SettingsChange.getSortDir(context)) {
            SortDirection.ASCENDING.id -> {
                when (sortType) {
                    SortType.ALPHABETICAL.id -> {
                        sortedFolders =
                            folders?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()
                        sortedFiles =
                            filteredFiles?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()
                    }

                    SortType.AUTHOR.id ->
                        sortedFiles =
                            filteredFiles?.sortedBy { getArtistFromFile(it) }?.toTypedArray()
                                ?: emptyArray()

                    SortType.LENGTH.id ->
                        sortedFiles =
                            filteredFiles?.sortedBy { getDurationFromFile(context, it.path) }
                                ?.toTypedArray() ?: emptyArray()

                }
            }

            SortDirection.DESCENDING.id -> {
                when (sortType) {
                    SortType.ALPHABETICAL.id -> {
                        sortedFolders =
                            folders?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()
                        sortedFiles = filteredFiles?.sortedByDescending { it.name }
                            ?.toTypedArray() ?: emptyArray()
                    }

                    SortType.AUTHOR.id -> sortedFiles =
                        filteredFiles?.sortedByDescending { getArtistFromFile(it) }?.toTypedArray()
                            ?: emptyArray()

                    SortType.LENGTH.id -> sortedFiles =
                        filteredFiles?.sortedByDescending { getDurationFromFile(context, it.path) }
                            ?.toTypedArray() ?: emptyArray()
                }
            }
        }

        if (sortType == SortType.RANDOM.id) {
            sortedFolders = folders?.shuffled()?.toTypedArray() ?: emptyArray()
            sortedFiles = filteredFiles?.shuffled()?.toTypedArray() ?: emptyArray()
        }

        return sortedFolders.plus(sortedFiles)

    }

    /**
     * Gets the name of the artist from a file.
     *
     * @param file The audio file to get the author from.
     * @return A string containing the author of the given audio file.
     */
    fun getArtistFromFile(file: File): String {
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