package com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu.Objects

import java.io.File

/**
 * This is a class for audio files when they are in the player queue. This is used in the adapter
 * to be able to move items around simpler. Because of the queue being comprised of the current
 * song, the queue, and the rest of the playlist, managing the current indexes and updating
 * consistently is challenging.
 *
 * A data class is a class with no methods and just variables. Just an object with setters
 * and getters with nothing else.
 *
 * @author Emmett Grebe
 * @version 7-10-2026
 */
data class QueueRowItem(
    val file: File,
    val isCurrentPlaying: Boolean,
    val isInQueue: Boolean
)