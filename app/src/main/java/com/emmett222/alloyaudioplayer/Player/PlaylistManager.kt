package com.emmett222.alloyaudioplayer.Player
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu.Objects.QueueRowItem
import com.emmett222.alloyaudioplayer.Player.PlayerActivity.Companion.onFileChangeListener
import java.io.File

/**
 * Manages the audio files in a playlist to play them in order. Only one PlaylistManager may be
 * active at a time.
 *
 * @author Emmett Grebe
 * @version 8-2-2026
 */
object PlaylistManager {
    lateinit var audioFile: File
    lateinit var allFiles: MutableList<File>
    lateinit var unShuffledAllFiles: MutableList<File>
    var audioQueue: ArrayDeque<File> = ArrayDeque()

    var currentPosition = -1
    var repeatOneOn: Boolean = false
    var shuffleOn: Boolean = false
    var repeatPlaylistOn: Boolean = false

    /**
     * Updates the player to play a new song.
     */
    fun playNewSong(file: File) {
        this.audioFile = file
        this.currentPosition = allFiles.indexOf(file)
        onFileChangeListener?.invoke(this.audioFile)
    }

    /**
     * Helper method to set up the files and playlist of files.
     *
     * @param file The audio file to be played.
     */
    fun setupFiles(file: File) {
        this.audioFile = file
        onFileChangeListener?.invoke(this.audioFile)

        this.allFiles = this.audioFile.parentFile?.listFiles { file -> (!file.isDirectory) && (file != null) }?.toMutableList() ?: mutableListOf<File>()
        this.unShuffledAllFiles = this.audioFile.parentFile?.listFiles { file -> (!file.isDirectory) && (file != null) }?.toMutableList() ?: mutableListOf<File>()

        this.currentPosition = allFiles.indexOf(audioFile)
    }

    /**
     * Removes a song.
     *
     * @param removed The file to remove from the list.
     * @param isInQueue If the file is in the queue or not.
     */
    fun remove(removed: File, isInQueue: Boolean) {
        if (isInQueue) {
            audioQueue.remove(removed)
            allFiles.remove(removed)
        } else {
            allFiles.remove(removed)
            unShuffledAllFiles.remove(removed)
        }
        currentPosition = allFiles.indexOf(audioFile)

    }

    /**
     * Changes the playlist to a new one. Typically used for when items are moved around.
     *
     * @param newList: The new list to change to.
     */
    fun changeList(newList: List<QueueRowItem>) {
        // Build a clean queue stream by filtering for items flagged as queue entries
        val newQueue = ArrayDeque<File>()
        newList.filter { it.isInQueue }.forEach { newQueue.add(it.file) }
        audioQueue = newQueue

        // Build the unplayed playlist tracks by filtering for standard tracks
        val remainingPlaylistTracks = newList
            .filter { !it.isCurrentPlaying && !it.isInQueue }
            .map { it.file }

        val historyTracks = allFiles.subList(0, currentPosition + 1)
        allFiles = (historyTracks + remainingPlaylistTracks).toMutableList()

        currentPosition = allFiles.indexOf(audioFile)
    }

    /**
     * Shuffles the playlist. Places the old playlist into another list so it can be put back if
     * unshuffled.
     */
    fun shuffle() {
        val mutablePlaylist = unShuffledAllFiles.toMutableList()

        mutablePlaylist.remove(audioFile) // Pull out the active song
        mutablePlaylist.shuffle()         // Shuffle the rest of the files
        mutablePlaylist.add(
            0,
            audioFile
        ) // Drop the active song right at the front (Index 0)

        this.allFiles = mutablePlaylist
        this.currentPosition = 0
    }

    /**
     * Unshuffles the playlist to its original state.
     */
    fun unshuffle() {
        this.allFiles.clear()
        this.allFiles.addAll(unShuffledAllFiles)
        this.currentPosition = allFiles.indexOf(audioFile)
    }

    /**
     * Skips to the next song in the playlist.
     *
     * @return The next audio file to play if there is one. If not, then null.
     */
    fun skipForward() : File? {
        // Skip through queue
        if (audioQueue.isNotEmpty()) {
            val newSong = audioQueue.removeFirst()
            playNewSong(newSong)
            return newSong
        }

        if ((currentPosition + 1 >= allFiles.size) && !repeatPlaylistOn) {
            return null
        }

        if (currentPosition + 1 >= allFiles.size) {
            currentPosition = 0
        } else {
            currentPosition++
        }
        playNewSong(allFiles[currentPosition])
        return allFiles[currentPosition]
    }

    /**
     * Skips backwards one song.
     * If song is more than 10 seconds in, start current song over. If not:
     * If repeat playlist is on and the player is on the first song, skip backwards will take player
     * to last song. If not, it does not do this.
     *
     * @return The next audio file to play if there is one. If not, then null.
     */
    fun skipBackward() : File?  {
        // If the song is the first in the list and repeat playlist is not toggled, do nothing.
        if ((currentPosition == 0) && !repeatPlaylistOn) {
            return null
        }

        // Skip back around if repeat playlist is toggled.
        if (currentPosition == 0) {
            currentPosition = (allFiles.size - 1)
            // Just go back one.
        } else {
            currentPosition--
        }
        playNewSong(allFiles[currentPosition])
        return allFiles[currentPosition]
    }
}