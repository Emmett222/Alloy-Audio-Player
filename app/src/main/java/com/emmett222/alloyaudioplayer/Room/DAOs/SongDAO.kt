package com.emmett222.alloyaudioplayer.Room.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emmett222.alloyaudioplayer.Room.Entities.Song
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for Songs. SQL commands defined here. Takes code and translates it to SQL.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Dao
interface SongDAO {
    @Query("SELECT * FROM song_table ORDER BY id ASC")
    fun allSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    suspend fun updateSong(song: Song)

    suspend fun deleteSong(song: Song)
}