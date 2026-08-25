package com.emmett222.alloyaudioplayer.Room.Databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emmett222.alloyaudioplayer.Room.DAOs.SongDAO
import com.emmett222.alloyaudioplayer.Room.Entities.Song

/**
 * Database for songs. Ties tables and DAOs together. Serves as the connection point to SQLite
 * database.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class SongDatabase: RoomDatabase() {
    abstract fun songDao(): SongDAO

    companion object {
        @Volatile
        private var INSTANCE: SongDatabase? = null

        fun getDatabase(context: Context): SongDatabase
        {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SongDatabase::class.java,
                    "song_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}