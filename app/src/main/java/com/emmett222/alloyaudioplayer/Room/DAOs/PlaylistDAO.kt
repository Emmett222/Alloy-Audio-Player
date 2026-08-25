package com.emmett222.alloyaudioplayer.Room.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emmett222.alloyaudioplayer.Room.Entities.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for Playlists. SQL commands defined here. Takes code and translates it to SQL.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Dao
interface PlaylistDAO {
    @Query("SELECT * FROM playlist_table ORDER BY id ASC")
    fun allPlaylists(): Flow<Playlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    suspend fun updatePlaylist(playlist: Playlist)

    suspend fun deletePlaylist(playlist: Playlist)
}