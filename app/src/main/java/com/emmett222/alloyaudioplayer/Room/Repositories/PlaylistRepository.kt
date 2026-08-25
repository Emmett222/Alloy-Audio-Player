package com.emmett222.alloyaudioplayer.Room.Repositories

import androidx.annotation.WorkerThread
import com.emmett222.alloyaudioplayer.Room.DAOs.PlaylistDAO
import com.emmett222.alloyaudioplayer.Room.Entities.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Repository for playlists. Middleman between UI and DAOs. Forces data operations to happen on
 * background threads so the UI does not freeze on reading large databases.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
class PlaylistRepository(private val playlistDAO: PlaylistDAO) {
    val allPlaylists: Flow<List<Playlist>> = playlistDAO.allPlaylists()

    @WorkerThread
    suspend fun insertPlaylist(playlist: Playlist) {
        playlistDAO.insertPlaylist(playlist)
    }

    @WorkerThread
    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDAO.updatePlaylist(playlist)
    }

    @WorkerThread
    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDAO.deletePlaylist(playlist)
    }
}