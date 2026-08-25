package com.emmett222.alloyaudioplayer.Room.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.emmett222.alloyaudioplayer.Room.Entities.Playlist
import com.emmett222.alloyaudioplayer.Room.Repositories.PlaylistRepository
import kotlinx.coroutines.launch

/**
 * Fetches data from repository and holds onto it.
 * 
 * @author Emmett Grebe
 * @version 8-25-2026
 */
class PlaylistViewModel(private val repository: PlaylistRepository): ViewModel() {
    var playlistItems: LiveData<List<Playlist>> = repository.allPlaylists.asLiveData()

    fun addPlaylist(newPlaylist: Playlist) = viewModelScope.launch {
        repository.insertPlaylist(newPlaylist)
    }

    fun updatePlaylist(playlist: Playlist) = viewModelScope.launch {
        repository.updatePlaylist(playlist)
    }

    fun addView(playlist: Playlist) = viewModelScope.launch {
        repository.updatePlaylist(playlist)
    }
}