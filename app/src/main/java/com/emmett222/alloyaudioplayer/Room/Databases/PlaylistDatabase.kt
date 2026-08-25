package com.emmett222.alloyaudioplayer.Room.Databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emmett222.alloyaudioplayer.Room.DAOs.PlaylistDAO
import com.emmett222.alloyaudioplayer.Room.Entities.Song

/**
 * Database for playlists. Ties tables and DAOs together. Serves as the connection point to SQLite
 * database.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class PlaylistDatabase: RoomDatabase() {
    abstract fun playlistDao(): PlaylistDAO

    companion object {
        @Volatile
        private var INSTANCE: PlaylistDatabase? = null

        fun getDatabase(context: Context): PlaylistDatabase
        {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaylistDatabase::class.java,
                    "playlist_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}