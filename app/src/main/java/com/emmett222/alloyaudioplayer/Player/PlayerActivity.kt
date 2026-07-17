package com.emmett222.alloyaudioplayer.Player

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu.QueueAdapter
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.StartMenuAdapter
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.*
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu.Objects.QueueRowItem
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import kotlin.math.abs

/**
 * Player screen for Alloy Audio Player.
 *
 * @author Emmett Grebe
 * @version 7-17-2026
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
    lateinit var allFiles: MutableList<File>
    lateinit var unShuffledAllFiles: MutableList<File>
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
    private lateinit var menuFilesRecycler: RecyclerView

    /**
     * Runs on opening the view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.isOld = intent.getStringExtra("isOld") == "true"
        if (isOld) {
            setupFiles(MediaEngine.getCurrentFile())
        } else {
            setupFiles(File(intent.getStringExtra("path")))
        }

        visualizerView = findViewById(R.id.visScreen)
        menuGraphic = findViewById(R.id.menuContainer)
        menuRecycler = menuGraphic.findViewById(R.id.menuRecycler)
        menuVisRecycler = menuGraphic.findViewById(R.id.menuVisualizers)
        menuQueueRecycler = menuGraphic.findViewById(R.id.menuQueue)
        menuFilesRecycler = menuGraphic.findViewById(R.id.menuFiles)

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

            setupGestures()
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
                setupFiles(newFile) // Reset playlist arrays
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
     * Sets up custom gestures.
     */
    private fun setupGestures() {
        // The 'object : ' syntax is used to create an anonymous class. It is a one time object that
        // implements an interface or extends a class, without needing to create a new .kt file.
        // Think of it as "Create an (object) that acts like (:) this class/interface (____) and
        // let me customize it.
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, // Start
                e2: MotionEvent,  // End
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // Motion determining.
                // Swipe from top to bottom quickly.
                if ((e1.y < 200) && abs(diffY) > abs(diffX)) { // Top of screen downwards
                    if ((diffY > 150) && (abs(velocityY) > 150)) { // Far enough and fast enough.
                        finish() // Go out of the player.
                    }
                }

                return false
            }
        })

        // Intercept touches on the root view
        findViewById<View>(R.id.main).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // This replaces the default back button functionality.
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (inMenu) backToVis(visualizerView, menuGraphic)
                else finish()
            }
        }
        // This adds the custom call back to the dispatcher. The dispatcher is responsible for
        // handling the back click.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    /**
     * Helper method to set up the files and playlist of files.
     *
     * @param file The audio file to be played.
     */
    @OptIn(UnstableApi::class)
    private fun setupFiles(file: File) {
        this.audioFile = file
        onFileChangeListener?.invoke(this.audioFile)

        this.allFiles = this.audioFile.parentFile.listFiles({ file -> !file.isDirectory }).toMutableList()
        this.unShuffledAllFiles = this.audioFile.parentFile.listFiles({ file -> !file.isDirectory }).toMutableList()

        this.currentPosition = allFiles.indexOf(audioFile)
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

        if (isOld && !MediaEngine.getPaused()) {
            controller.pause()
            playBtn.setImageResource(R.drawable.btn_play)
            handler.removeCallbacks(updater)
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
                this.allFiles.clear()
                this.allFiles.addAll(unShuffledAllFiles)
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

                this.allFiles = mutablePlaylist
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
        menuFilesRecycler.layoutManager = LinearLayoutManager(applicationContext)

        makeVisMenu()
        makeFilesMenu(audioFile.parentFile.parentFile, audioFile.parentFile)

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
                StartMenuAdapter.FILES -> {
                    menuRecycler.visibility = View.INVISIBLE
                    menuFilesRecycler.visibility = View.VISIBLE
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
                menuFilesRecycler.visibility = View.INVISIBLE
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
     * Makes a new files menu.
     */
    private fun makeFilesMenu(backOption: File?, folder: File) {
        val rawFiles = folder.listFiles() ?: return
        val filteredFiles: Array<File> = rawFiles.filter { file ->
            file.isDirectory ||
                    file.extension.equals("mp3", true) ||
                    file.extension.equals("m4a", true) ||
                    file.extension.equals("opus", true) ||
                    file.extension.equals("aac", true) ||
                    file.extension.equals("aif", true) ||
                    file.extension.equals("aiff", true) ||
                    file.extension.equals("cda", true) ||
                    file.extension.equals("flac", true) ||
                    file.extension.equals("ogg", true) ||
                    file.extension.equals("wav", true)
        }.toTypedArray()
        // Whenever an item is clicked on the files menu, the start menu callback send the info
        // back to here. Uses the static global variables in the companion to determine which was
        // clicked.
        menuFilesRecycler.adapter = FilesMenuAdapter(this, backOption, filteredFiles) { clickedItem, isGoTo ->
            if (clickedItem.isDirectory) { // Folder.
                makeFilesMenu(clickedItem.parentFile, clickedItem)
            } else {
                if (isGoTo) {
                    setupFiles(clickedItem)
                } else {
                    audioQueue.addLast(clickedItem)
                }
            }
        }
    }

    /**
     * Makes a new queue menu. Replaces old queue menu with new one.
     *
     * @param queueItems Items from the queue.
     */
    private fun makeQueueMenu(queueItems: ArrayDeque<File>) {
        val masterList = mutableListOf<QueueRowItem>()
        // Add the active song
        masterList.add(QueueRowItem(this.audioFile, true, false))

        // Add the queue tracks
        queueItems.forEach { file ->
            masterList.add(QueueRowItem(file, false, true))
        }

        // Add the rest of the playlist tracks
        val nextIndex = currentPosition + 1
        if (nextIndex < allFiles.size) {
            for (i in nextIndex until allFiles.size) {
                masterList.add(QueueRowItem(allFiles[i], false, false))
            }
        }

        menuQueueRecycler.adapter = QueueAdapter(
            this, masterList,
            onItemClick = { clickedItem, ->
                    // User manually clicked a row item to force play it
                    val playlistIndex = allFiles.indexOf(clickedItem)
                    if (playlistIndex != -1) {
                        // Only move the pointer if they clicked a standard track from the playlist
                        currentPosition = playlistIndex
                    }

                    this.audioFile = clickedItem
                    setupControllerFile(clickedItem)
                    setupTitle(clickedItem.name)
                    makeQueueMenu(queueItems)
                },
            onQueueClick = { clickedItem, ->
                queueItems.addLast(clickedItem)
                makeQueueMenu(queueItems)
            },
            onRemoveClick = { clickedItem, isInQueue ->
                if (isInQueue) {
                    queueItems.remove(clickedItem)
                } else {
                    allFiles =
                        allFiles.filter { currFile -> currFile != clickedItem }.toMutableList()
                    unShuffledAllFiles =
                        unShuffledAllFiles.filter { currFile -> currFile != clickedItem }
                            .toMutableList()
                }
                makeQueueMenu(queueItems)
            },
            onItemMove = { finalModelList ->
                // Build a clean queue stream by filtering for items flagged as queue entries
                val newQueue = ArrayDeque<File>()
                finalModelList.filter { it.isInQueue }.forEach { newQueue.add(it.file) }
                this.audioQueue = newQueue

                // Build the unplayed playlist tracks by filtering for standard tracks
                val remainingPlaylistTracks = finalModelList
                    .filter { !it.isCurrentPlaying && !it.isInQueue }
                    .map { it.file }

                val historyTracks = allFiles.subList(0, currentPosition + 1)
                this.allFiles = (historyTracks + remainingPlaylistTracks).toMutableList()

                this.currentPosition = allFiles.indexOf(audioFile)

                // Update the adapter with the ALREADY sorted final model list
                val adapter = menuQueueRecycler.adapter as QueueAdapter
                adapter.updateData(finalModelList.toMutableList())
            }
        )

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
            this.audioFile = audioQueue.removeFirst()

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
     * Skips backwards one song.
     * If song is more than 10 seconds in, start current song over. If not:
     * If repeat playlist is on and the player is on the first song, skip backwards will take player
     * to last song. If not, it does not do this.
     */
    fun skipBackward() {
        // If the song is the first in the list and repeat playlist is not toggled, do nothing.
        if ((currentPosition == 0) && !repeatPlaylistOn) {
            return
        }
        if (controller.currentPosition > 10000) { // 10 Seconds
            controller.seekTo(0) // Go back to beginning.
            return
        }
        // Skip back around if repeat playlist is toggled.
        if (currentPosition == 0) {
            currentPosition = (allFiles.size - 1)
        // Just go back one.
        } else {
            currentPosition--
        }
        // Re-set everything back up.
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