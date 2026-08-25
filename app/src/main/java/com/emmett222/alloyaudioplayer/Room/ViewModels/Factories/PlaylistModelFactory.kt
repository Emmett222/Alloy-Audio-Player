package com.emmett222.alloyaudioplayer.Room.ViewModels.Factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emmett222.alloyaudioplayer.Room.Repositories.PlaylistRepository
import com.emmett222.alloyaudioplayer.Room.ViewModels.PlaylistViewModel

/**
 * Tells the OS how to construct a PlaylistViewModel. Normally Android creates empty ViewModels so this
 * is needed to have one with the repository in it
 *
 * @author Emmett Grebe
 * @version 8-25-2026
 */
class PlaylistModelFactory(private val repository: PlaylistRepository): ViewModelProvider.Factory {
    /**
     * Creates a PlaylistViewModel and returns it.
     *
     * @param modelClass The model class used to create the PlaylistViewModel.
     * @return A PlaylistViewModel.
     * @throws IllegalArgumentException When the class given is not a PlaylistViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            return PlaylistViewModel(repository) as T
        } else {
            throw IllegalArgumentException("Unknown class for Playlist Model!")
        }
    }
}