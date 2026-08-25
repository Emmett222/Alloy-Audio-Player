package com.emmett222.alloyaudioplayer.Room.Repositories

import androidx.annotation.WorkerThread
import com.emmett222.alloyaudioplayer.Room.DAOs.SongDAO
import com.emmett222.alloyaudioplayer.Room.Entities.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository for songs. Middleman between UI and DAOs. Forces data operations to happen on
 * background threads so the UI does not freeze on reading large databases.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
class SongRepository(private val songDAO: SongDAO) {
    val allSongs: Flow<List<Song>> = songDAO.allSongs()

    @WorkerThread
    suspend fun insertSong(song: Song) {
        songDAO.insertSong(song)
    }

    @WorkerThread
    suspend fun updateSong(song: Song) {
        songDAO.updateSong(song)
    }

    @WorkerThread
    suspend fun deleteSong(song: Song) {
        songDAO.deleteSong(song)
    }
}