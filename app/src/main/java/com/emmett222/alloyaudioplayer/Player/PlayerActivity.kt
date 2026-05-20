package com.emmett222.alloyaudioplayer.Player

import android.content.ComponentName
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.emmett222.alloyaudioplayer.Background.MediaEngine
import com.emmett222.alloyaudioplayer.Player.Graphic.VisualizerGraphic
import com.emmett222.alloyaudioplayer.R
import java.io.File

/**
 * Player screen for Alloy Audio Player.
 *
 * @author Emmett Grebe
 * @version 5-20-2026
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var audioFile: File
    lateinit var controller: MediaController
    var vis: Visualizer? = null // Nullable for later safety check.
    var isStart: Boolean = true;
    var repeatOneOn: Boolean = false;
    var shuffleOn: Boolean = false;
    var repeatPlaylistOn: Boolean = false;

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updater: Runnable

    /**
     * Runs on opening the view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.audioFile = File(intent.getStringExtra("path"))

        // This token is needed to connect to the service.
        val sessionToken = SessionToken(this, ComponentName(this, MediaEngine::class.java))
        // Why use a future? Because we need to wait for it to build.
        val controllerFuture = MediaController.Builder(this, sessionToken)
            .setConnectionHints(Bundle().apply {
                putBoolean("IS_GUI", true) // The secret password
            })
            .buildAsync()

        controllerFuture.addListener({
            // THIS CODE RUNS ONLY WHEN CONNECTED
            controller = controllerFuture.get()

            setupControllerFile()
            setupVisualizer()
            setupTitle()
            setupTime()
            setupPauseBtn()
            setupFastBtns()
            setupRepeatOneBtn()
            setupShuffleBtn()
            setupRepeatPlaylistBtn()

        }, ContextCompat.getMainExecutor(this))

        isStart = false;
    }

    /**
     * Runs when activity is ended. Kills the visualizer to avoid crashes.
     */
    override fun onDestroy() {
        vis?.enabled = false
        vis?.release()
        vis = null
        super.onDestroy()
    }

    /**
     * Helper method to setup the controller.
     */
    private fun setupControllerFile() {
        val retriever = MediaMetadataRetriever()
        var artistName = "Unknown Artist"

        try {
            retriever.setDataSource(audioFile.absolutePath)
            artistName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"
        } catch (e: Exception) {
            // Log error or handle missing file
        } finally {
            retriever.release()
        }

        val mediaItemWithMetadata =
            MediaItem.Builder().setUri(Uri.fromFile(audioFile)).setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audioFile.name)
                    .setArtist(artistName)
                    .setArtworkUri("android.resource://com.emmett222.alloyaudioplayer/drawable/background".toUri())
                    .build()
            ).build()

        controller.setMediaItem(mediaItemWithMetadata)
        controller.prepare()
        controller.play()
    }

    /**
     * Sets up the audio visualizer.
     */
    @OptIn(UnstableApi::class)
    private fun setupVisualizer() {
        val visualizerView = findViewById<VisualizerGraphic>(R.id.visScreen)
        visualizerView.change(VisualizerGraphic.TYPE_BARS)

        var currentActiveSessionId = -1

        controller.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = controller.sessionExtras.getInt("AUDIO_SESSION_ID", 0)

                    // Guard: Only run with valid ID and it's from the running one.
                    // This is so it doesn't crash when user seek ahead.
                    if (sessionId > 0 && sessionId != currentActiveSessionId) {

                        // Clean up.
                        vis?.enabled = false
                        vis?.release()
                        vis = null

                        // Making a new visualizer because we cannot directly alter vis. Kotlin
                        // doesn't trust that vis will *stay* non-null between where we made it,
                        // and where we alter it. Because of this, it will not compile.
                        val newVis = Visualizer(sessionId)
                        newVis.captureSize = 1024

                        // Listens for changes on data change. Listens for waveform,
                        // and data capture.
                        val captureListener: Visualizer.OnDataCaptureListener = object :
                            Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (waveform != null) {
                                    visualizerView.updateWaveform(waveform)
                                }
                            }

                            override fun onFftDataCapture(
                                visualizer: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                // Unused for now.
                            }
                        }
                        newVis.setDataCaptureListener(
                            captureListener,
                            Visualizer.getMaxCaptureRate() / 2,
                            true,
                            false
                        )
                        newVis.enabled = true
                        vis = newVis
                        currentActiveSessionId = sessionId // Mark session ID for guard later.
                    }
                }
            }
        })
    }

    /**
     * Helper method to setup the time views on load.
     */
    private fun setupTime() {
        val endText: TextView = findViewById(R.id.endNum)
        val seekBar: SeekBar = findViewById(R.id.timeSeekBar)

        // Watches if the player changes.
        // Listen if the playback state changes.
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // The player has loaded the file
                        val duration = controller.duration.toInt()

                        // Now it is safe to set these
                        seekBar.max = duration
                        endText.text = formatMinutesAndSeconds(duration)

                        // Start the UI updater loop now that we have a max
                        handler.post(updater)
                    }

                    Player.STATE_ENDED -> {
                        handler.removeCallbacks(updater)
                    }
                }
            }
        }

        controller.addListener(playerListener)

        updater = Runnable {
            val currentPos = controller.currentPosition.toInt()
            seekBar.progress = currentPos
            changeTime(currentPos)

            handler.postDelayed(updater, 500)
        }

        // OnSeekBarChangeListener is like an interface. If you want to listen to seekbar, you must
        // do all 3 methods.
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    controller.seekTo(progress.toLong())
                    changeTime(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Helper method to setup the pause button on load.
     */
    private fun setupPauseBtn() {
        var playBtn: ImageButton = findViewById(R.id.playBtn)
        playBtn.setOnClickListener {
            if (controller.isPlaying == true) {
                controller.pause()
                playBtn.setImageResource(R.drawable.btn_play)
                handler.removeCallbacks(updater)
            } else {
                controller.play()
                playBtn.setImageResource(R.drawable.btn_pause)
                handler.post(updater)
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to setup the fast forward and fast rewind buttons.
     */
    private fun setupFastBtns() {
        var ffBtn: ImageButton = findViewById(R.id.fastForward)
        var frBtn: ImageButton = findViewById(R.id.fastRewind)

        ffBtn.setOnClickListener {
            // Go forward 1 minute.
            controller.seekTo(controller.currentPosition + 60000)
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        frBtn.setOnClickListener {
            // Rewind 1 minute.
            controller.seekTo(controller.currentPosition - 60000)
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to setup the repeat one button on load.
     */
    private fun setupRepeatOneBtn() {
        var repeatOneBtn: ImageButton = findViewById(R.id.repeatOneBtn)
        Player.REPEAT_MODE_OFF;
        repeatOneBtn.setOnClickListener {
            if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
                controller.repeatMode = Player.REPEAT_MODE_OFF;
                repeatOneBtn.setImageResource(R.drawable.btn_repeat1off)
                repeatOneOn = false;
            } else {
                controller.repeatMode = Player.REPEAT_MODE_ONE;
                repeatOneBtn.setImageResource(R.drawable.btn_repeat1on)
                repeatOneOn = true;
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to setup the shuffle button on load.
     * Right now, shuffle does nothing until the playlists are made.
     */
    private fun setupShuffleBtn() {
        var shuffleBtn: ImageButton = findViewById(R.id.shuffleBtn)
        shuffleBtn.setOnClickListener {
            if (shuffleOn) {
                shuffleBtn.setImageResource(R.drawable.btn_shuffleoff)
                shuffleOn = false;
            } else {
                shuffleBtn.setImageResource(R.drawable.btn_shuffleon)
                shuffleOn = true;
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to setup the repeat playlist button on load.
     * Right now, repeat playlist does nothing until the playlists are made.
     */
    private fun setupRepeatPlaylistBtn() {
        var repeatPlaylistBtn: ImageButton = findViewById(R.id.repeatBtn)
        repeatPlaylistBtn.setOnClickListener {
            if (repeatPlaylistOn) {
                repeatPlaylistBtn.setImageResource(R.drawable.btn_repeatplaylistoff)
                repeatPlaylistOn = false;
            } else {
                repeatPlaylistBtn.setImageResource(R.drawable.btn_repeatplayliston)
                repeatPlaylistOn = true;
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to setup scrolling title on load.
     */
    private fun setupTitle() {
        var titleString: TextView = findViewById(R.id.titleString)
        titleString.text = audioFile.name

        titleString.postDelayed({ // Only fires when the title is loaded.
            titleString.isSelected = true // So the marquee starts on load,
        }, 2000) // But waits two seconds before moving.
    }

    /**
     * Changes the time on the left timer.
     * @param m Milliseconds.
     */
    fun changeTime(m: Int) {
        var currentTime: TextView = findViewById(R.id.currentNum)
        currentTime.text = formatMinutesAndSeconds(m)
    }

    /**
     * Format milliseconds to minutes and seconds.
     * @param m Time in milliseconds.
     * @return String of the time in the format of x:xx.
     */
    fun formatMinutesAndSeconds(m: Int): String {
        val minutes = (m / 1000) / 60
        val seconds = (m / 1000) % 60

        // Formats to x:xx.
        return String.format("%d:%02d", minutes, seconds)
    }
}