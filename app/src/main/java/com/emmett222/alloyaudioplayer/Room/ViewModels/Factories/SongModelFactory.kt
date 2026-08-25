package com.emmett222.alloyaudioplayer.Room.ViewModels.Factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emmett222.alloyaudioplayer.Room.Repositories.SongRepository
import com.emmett222.alloyaudioplayer.Room.ViewModels.SongViewModel

/**
 * Tells the OS how to construct a SongViewModel. Normally Android creates empty ViewModels so this
 * is needed to have one with the repository in it
 *
 * @author Emmett Grebe
 * @version 8-25-2026
 */
class SongModelFactory(private val repository: SongRepository): ViewModelProvider.Factory {
    /**
     * Creates a SongViewModel and returns it.
     *
     * @param modelClass The model class used to create the SongViewModel.
     * @return A SongViewModel.
     * @throws IllegalArgumentException When the class given is not a SongViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            return SongViewModel(repository) as T
        } else {
            throw IllegalArgumentException("Unknown class for Song Model!")
        }
    }
}