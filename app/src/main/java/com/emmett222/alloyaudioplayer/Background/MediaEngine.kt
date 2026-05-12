package com.emmett222.alloyaudioplayer.Background

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.emmett222.alloyaudioplayer.R

/**
 * Background service for audio playing.
 *
 * @author Emmett Grebe
 * @version 5-12-2026
 */
class MediaEngine : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var mediaPlayer: Player

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // The default provider automatically handles the notification channel,
        // the 3-button layout, and the album art background extraction.
        val provider = DefaultMediaNotificationProvider.Builder(this).build()
        provider.setSmallIcon(R.drawable.music_cast_24px)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) {
            initializeSession()
        }
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    private fun initializeSession() {
        val basePlayer = ExoPlayer.Builder(this).build()

        // Wrap the player to force the Skip Next and Skip Previous buttons to stay visible
        mediaPlayer = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return getAvailableCommands().contains(command)
            }
        }

        mediaSession = MediaSession.Builder(this, mediaPlayer)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.run {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }
            release()
            mediaSession = null
        }
        super.onTaskRemoved(rootIntent)
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {

            if (controller.connectionHints.getBoolean("IS_GUI", false)) {
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            } else {
                val playerCommandsBuilder = Player.Commands.Builder()

                // Grant all standard permissions for the 3 buttons and the seekbar
                playerCommandsBuilder.addAll(session.player.availableCommands)
                playerCommandsBuilder.add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(playerCommandsBuilder.build())
                    .build()
            }
        }
    }
}