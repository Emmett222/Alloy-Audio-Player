package com.emmett222.alloyaudioplayer.Room

import android.app.Application
import com.emmett222.alloyaudioplayer.Room.Databases.PlaylistDatabase
import com.emmett222.alloyaudioplayer.Room.Databases.SongDatabase
import com.emmett222.alloyaudioplayer.Room.Repositories.PlaylistRepository
import com.emmett222.alloyaudioplayer.Room.Repositories.SongRepository

/**
 * Application for songs and playlists. Gets the repositories for both. This is a Singleton like
 * Application() because opening and closing database connections is heavy on RAM and battery.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
class MediaApplication: Application() {
    private val songDatabase by lazy { SongDatabase.getDatabase(this) }
    val songRepository by lazy { SongRepository(songDatabase.songDao()) }

    private val playlistDatabase by lazy { PlaylistDatabase.getDatabase(this) }
    val playlistRepository by lazy { PlaylistRepository(playlistDatabase.playlistDao()) }
}