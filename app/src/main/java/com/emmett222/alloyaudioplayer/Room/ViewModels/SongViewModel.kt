package com.emmett222.alloyaudioplayer.Room.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.emmett222.alloyaudioplayer.Room.Entities.Song
import com.emmett222.alloyaudioplayer.Room.Repositories.SongRepository
import kotlinx.coroutines.launch

/**
 * Fetches data from repository and holds onto it.
 *
 * @author Emmett Grebe
 * @version 8-25-2026
 */
class SongViewModel(private val repository: SongRepository): ViewModel() {
    var songItems: LiveData<List<Song>> = repository.allSongs.asLiveData()

    fun addSong(newSong: Song) = viewModelScope.launch {
        repository.insertSong(newSong)
    }

    fun updateSong(song: Song) = viewModelScope.launch {
        repository.updateSong(song)
    }

    fun addToViewCount(song: Song) = viewModelScope.launch {
        song.views ++
        repository.updateSong(song)
    }
}