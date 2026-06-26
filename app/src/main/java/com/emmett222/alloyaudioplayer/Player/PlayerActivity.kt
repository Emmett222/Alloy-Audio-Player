package com.emmett222.alloyaudioplayer.Player

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Background.MediaEngine
import com.emmett222.alloyaudioplayer.Player.Graphic.BaseGraphic
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueAdapter
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.StartMenuAdapter
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.*
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File

/**
 * Player screen for Alloy Audio Player.
 *
 * @author Emmett Grebe
 * @version 6-26-2026
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        // These are companion callbacks.
        var onFileChangeListener: ((File) -> Unit)? = null
    }

    /**
     * vvvvv ---------- Files ---------- vvvvv
     */
    lateinit var audioFile: File
    lateinit var allFiles: Array<File>
    lateinit var unShuffledAllFiles: Array<File>
    var currentPosition = -1
    var audioQueue: ArrayDeque<File> = ArrayDeque<File>()

    /**
     * vvvvv ---------- Player ---------- vvvvv
     */
    lateinit var controller: MediaController
    var vis: Visualizer? = null // Nullable for later safety check.

    /**
     * vvvvv ---------- Status ---------- vvvvv
     */
    var isOld: Boolean = false
    var isStart: Boolean = true;
    var repeatOneOn: Boolean = false;
    var shuffleOn: Boolean = false;
    var justShuffled: Boolean = false
    var repeatPlaylistOn: Boolean = false;
    var inMenu: Boolean = false

    /**
     * vvvvv ---------- Graphics ---------- vvvvv
     */
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updater: Runnable
    private lateinit var visualizerView: BaseGraphic
    private lateinit var menuGraphic: ConstraintLayout
    private lateinit var menuRecycler: RecyclerView
    private lateinit var menuVisRecycler: RecyclerView
    private lateinit var menuQueueRecycler: RecyclerView

    /**
     * Runs on opening the view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.isOld = intent.getStringExtra("isOld") == "true"
        setupFiles()

        visualizerView = findViewById(R.id.visScreen)
        menuGraphic = findViewById(R.id.menuContainer)
        menuRecycler = menuGraphic.findViewById(R.id.menuRecycler)
        menuVisRecycler = menuGraphic.findViewById(R.id.menuVisualizers)
        menuQueueRecycler = menuGraphic.findViewById(R.id.menuQueue)

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

            setupControllerFile(audioFile)
            setupFastBtns()
            setupMenuBtn()
            setupShuffleBtn()
            setupSkipBtns()
            setupTime()
            setupTitle(audioFile.name)
            setupPauseBtn()
            setupRepeatOneBtn()
            setupRepeatPlaylistBtn()
            setupVisualizer()

        }, ContextCompat.getMainExecutor(this))

        isStart = false;
    }

    /**
     * This runs when a new intent is made for this activity. This is set up in a way to do nothing
     * if the "path" extra is null. This is so this activity can be reopened with all of it's data
     * inside.
     *
     * @param intent The Intent to use for a new activity on new song being opened.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val newPath = intent.getStringExtra("path")
        if (newPath != null) {
            val newFile = File(newPath)

            // Only interrupt playback if it's actually a new song
            if (!::audioFile.isInitialized || audioFile.absolutePath != newFile.absolutePath) {
                setupFiles() // Reset playlist arrays
                setupControllerFile(audioFile) // Tell Media3 to play the new song
                setupTitle(audioFile.name)
            }
        }
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
     * Helper method to set up the files and playlist of files.
     */
    @OptIn(UnstableApi::class)
    private fun setupFiles() {
        if (isOld) {
            this.audioFile = MediaEngine.getCurrentFile()
        } else {
            this.audioFile = File(intent.getStringExtra("path"))
        }

        onFileChangeListener?.invoke(this.audioFile)

        this.allFiles = this.audioFile.parentFile.listFiles({ file -> !file.isDirectory })
        this.unShuffledAllFiles = this.allFiles.clone()

        // withIndex() is like an iterator, but it keeps track of the index.
        for ((i, f) in this.allFiles.withIndex()) {
            if (f == audioFile) {
                this.currentPosition = i
                break;
            }
        }
    }

    /**
     * Helper method to set up the controller.
     */
    private fun setupControllerFile(newFile: File) {
        val retriever = MediaMetadataRetriever()
        var artistName = "Unknown Artist"

        try {
            retriever.setDataSource(newFile.absolutePath)
            artistName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"
        } catch (e: Exception) {
            // Log error or handle missing file
        } finally {
            retriever.release()
        }

        val mediaItemWithMetadata =
            MediaItem.Builder().setUri(Uri.fromFile(newFile))
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(Uri.fromFile(newFile))
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(newFile.name)
                        .setArtist(artistName)
                        .setArtworkUri("android.resource://com.emmett222.alloyaudioplayer/drawable/background".toUri())
                        .build()
            ).build()

        controller.setMediaItem(mediaItemWithMetadata)
        controller.prepare()
        controller.play()
    }

    /**
     * vvvvv -------------------- Setups -------------------- vvvvv
     */

    /**
     * Sets up the audio visualizer.
     */
    @OptIn(UnstableApi::class)
    private fun setupVisualizer() {
        visualizerView.changeScreen(5) // later change this for settings.

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
                                // Unused now
                            }

                            override fun onFftDataCapture(
                                visualizer: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (fft != null) {
                                    visualizerView.updateFFT(fft)
                                }
                            }
                        }
                        newVis.setDataCaptureListener(
                            captureListener,
                            Visualizer.getMaxCaptureRate() / 2,
                            false,
                            true
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
     * Helper method to set up the time views on load.
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
                        skipForward()
                    }
                    Player.STATE_BUFFERING -> {
                        // Unused for now.
                    }
                    Player.STATE_IDLE -> {
                        // Unused for now.
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

        if (isOld) {
            controller.seekTo(MediaEngine.getCurrentPosition())
        }
    }

    /**
     * Helper method to set up the pause button on load.
     */
    private fun setupPauseBtn() {
        val playBtn: ImageButton = findViewById(R.id.playBtn)
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
     * Helper method to set up the fast-forward and fast rewind buttons.
     */
    private fun setupFastBtns() {
        val ffBtn: ImageButton = findViewById(R.id.fastForward)
        val frBtn: ImageButton = findViewById(R.id.fastRewind)

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

    private fun setupSkipBtns() {
        val skipFBtn: ImageButton = findViewById(R.id.skipForwardBtn)
        val skipBBtn: ImageButton = findViewById(R.id.skipBackBtn)

        skipFBtn.setOnClickListener {
            skipForward()
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        skipBBtn.setOnClickListener {
            skipBackward()
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to set up the repeat one button on load.
     */
    private fun setupRepeatOneBtn() {
        val repeatOneBtn: ImageButton = findViewById(R.id.repeatOneBtn)
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
     * Helper method to set up the shuffle button on load.
     */
    private fun setupShuffleBtn() {
        val shuffleBtn: ImageButton = findViewById(R.id.shuffleBtn)
        shuffleBtn.setOnClickListener {
            if (shuffleOn) {
                this.allFiles = unShuffledAllFiles.clone()
                this.currentPosition = allFiles.indexOf(audioFile)
                shuffleBtn.setImageResource(R.drawable.btn_shuffleoff)
                shuffleOn = false;
            } else {
                val mutablePlaylist = unShuffledAllFiles.toMutableList()

                mutablePlaylist.remove(audioFile) // Pull out the active song
                mutablePlaylist.shuffle()         // Shuffle the rest of the files
                mutablePlaylist.add(
                    0,
                    audioFile
                ) // Drop the active song right at the front (Index 0)

                this.allFiles = mutablePlaylist.toTypedArray()
                this.currentPosition = 0

                shuffleBtn.setImageResource(R.drawable.btn_shuffleon)
                shuffleOn = true;
            }
            this.currentPosition = allFiles.indexOf(audioFile)
            makeQueueMenu(audioQueue)
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Sets up the menu button.
     */
    private fun setupMenuBtn() {
        val menuBtn: ImageButton = findViewById(R.id.menuBtn)
        val visGraphic: BaseGraphic = findViewById(R.id.visScreen)

        menuRecycler.layoutManager = LinearLayoutManager(applicationContext)
        menuVisRecycler.layoutManager = LinearLayoutManager(applicationContext)
        menuQueueRecycler.layoutManager = LinearLayoutManager(applicationContext)

        makeVisMenu()

        // Whenever an item is clicked on the start menu, the start menu callback send the info
        // back to here. Uses the static global variables in the companion to determine which was
        // clicked.
        menuRecycler.adapter = StartMenuAdapter(applicationContext) { clickedItem ->
            when (clickedItem) {
                StartMenuAdapter.VISUALIZERS -> {
                    menuRecycler.visibility = View.INVISIBLE
                    menuVisRecycler.visibility = View.VISIBLE
                }
                StartMenuAdapter.QUEUE -> {
                    menuRecycler.visibility = View.INVISIBLE
                    makeQueueMenu(audioQueue)
                    menuQueueRecycler.visibility = View.VISIBLE
                }
            }
        }

        menuBtn.setOnClickListener {
            if (inMenu) {
                // If in menu, turn off menu to show visualizer.
                backToVis(visGraphic, menuGraphic)
            } else {
                // If not in menu, turn off visualizer to show menu.
                visGraphic.visibility = View.INVISIBLE
                menuGraphic.visibility = View.VISIBLE

                menuRecycler.visibility = View.VISIBLE
                menuVisRecycler.visibility = View.INVISIBLE
                menuQueueRecycler.visibility = View.INVISIBLE
                inMenu = true
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Makes a new visualizer menu.
     */
    private fun makeVisMenu() {
        // Whenever an item is clicked on the visualizer menu, the start menu callback send the info
        // back to here. Uses the static global variables in the companion to determine which was
        // clicked.
        menuVisRecycler.adapter = VisualizerMenuAdapter(applicationContext) { clickedItem ->
            when (clickedItem) {
                VisualizerMenuAdapter.NOVIS -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_NONE)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.LINEWAVE -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_WAVE)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.MIRLINEWAVE -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_MIRROR_WAVE)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.LINEBARS -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_BARS)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.BOTLINEBARS -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_BOTTOM_BARS)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.CIRCLEWAVE -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_CIRCLE_WAVE)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.CIRCLEBAR -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_CIRCLE_BARS)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.CIRCLEGROW -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_CIRCLE_GROW)
                    backToVis(visualizerView, menuGraphic)
                }
                VisualizerMenuAdapter.TALKINGSMILEY -> {
                    visualizerView.changeScreen(BaseGraphic.VIS_TYPE_SMILEY)
                    backToVis(visualizerView, menuGraphic)
                }
            }
            if (clickedItem == VisualizerMenuAdapter.NOVIS) {
                visualizerView.setBackgroundColor(Color.parseColor("#082107"))
            } else {
                visualizerView.setBackgroundColor(Color.parseColor("#0d380c"))
            }
        }
    }

    /**
     * Makes a new queue menu. Replaces old queue menu with new one.
     *
     * @param queueItems Items from the queue.
     */
    private fun makeQueueMenu(queueItems: ArrayDeque<File>) {
        val items: Array<File> = allFiles.sliceArray(currentPosition..<allFiles.size)
        menuQueueRecycler.adapter = QueueAdapter(this, queueItems, items)
        { clickedItem, isAddQueue, isRemove, isInQueue ->
            if (!isAddQueue && !isRemove) {
                currentPosition = allFiles.indexOf(clickedItem)
                this.audioFile = clickedItem
                setupControllerFile(clickedItem)
                setupTitle(clickedItem.name)
            }
            if (isAddQueue) {
                queueItems.addLast(clickedItem)
            }
            if (isRemove) {
                if (isInQueue) {
                    queueItems.remove(clickedItem)
                } else {
                    allFiles =
                        allFiles.filter { currFile -> currFile != clickedItem }.toTypedArray()
                    unShuffledAllFiles =
                        unShuffledAllFiles.filter { currFile -> currFile != clickedItem }
                            .toTypedArray()
                }
            }
            makeQueueMenu(queueItems)
        }
    }


    /**
     * Helper method to set up the repeat playlist button on load.
     * Right now, repeat playlist does nothing until the playlists are made.
     */
    private fun setupRepeatPlaylistBtn() {
        val repeatPlaylistBtn: ImageButton = findViewById(R.id.repeatBtn)
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
     * Helper method to set up scrolling title on load.
     */
    private fun setupTitle(title: String) {
        val titleString: TextView = findViewById(R.id.titleString)
        titleString.text = NameUtil.removeDescriptors(title)

        titleString.postDelayed({ // Only fires when the title is loaded.
            titleString.isSelected = true // So the marquee starts on load,
        }, 2000) // But waits two seconds before moving.
    }

    /**
     * vvvvv -------------------- Helpers -------------------- vvvvv
     */

    /**
     * Skips forward one song. If repeat playlist is on and the player is on the last song, it goes
     * back to the beginning of the playlist. If not, does not skip.
     */
    fun skipForward() {
        // Skip through queue
        if (audioQueue.isNotEmpty()) {
            this.audioFile = audioQueue.first()
            audioQueue.removeFirst()

            val newPos = allFiles.indexOf(this.audioFile)
            if (newPos != -1) {
                this.currentPosition = newPos
            }

            makeQueueMenu(audioQueue)
            setupControllerFile(this.audioFile)
            setupTitle(this.audioFile.name)
            return
        }

        if ((currentPosition + 1 >= allFiles.size) && !repeatPlaylistOn) {
            return
        }

        if (currentPosition + 1 >= allFiles.size) {
            currentPosition = 0
        } else {
            currentPosition++
        }
        this.audioFile = allFiles[currentPosition]
        makeQueueMenu(audioQueue)
        setupControllerFile(this.audioFile)
        setupTitle(this.audioFile.name)
    }

    /**
     * Skips backwards one song. If repeat playlist is on and the player is on the first song, skip
     * backwards will take player to last song. If not, it does not do this.
     */
    fun skipBackward() {
        // If the song is the first in the list and repeat playlist is not toggled, do nothing.
        if ((currentPosition == 0) && !repeatPlaylistOn) {
            return
        }
        if (currentPosition == 0) {
            currentPosition = (allFiles.size - 1)
        } else {
            currentPosition--
        }
        this.audioFile = allFiles[currentPosition]
        makeQueueMenu(audioQueue)
        setupControllerFile(this.audioFile)
        setupTitle(this.audioFile.name)
    }

    /**
     * Helper function to go back to visualizer.
     */
    private fun backToVis(visGraphic: BaseGraphic, menuGraphic: ConstraintLayout) {
        menuGraphic.visibility = View.INVISIBLE
        visGraphic.visibility = View.VISIBLE
        inMenu = false
    }

    /**
     * Changes the time on the left timer.
     * @param m Milliseconds.
     */
    fun changeTime(m: Int) {
        val currentTime: TextView = findViewById(R.id.currentNum)
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