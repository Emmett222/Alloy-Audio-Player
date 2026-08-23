package com.emmett222.alloyaudioplayer.Player

import android.content.ComponentName
import android.content.Intent
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
import androidx.core.graphics.drawable.toDrawable
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
import androidx.core.graphics.toColorInt
import com.emmett222.alloyaudioplayer.FileList.FileListActivity
import com.emmett222.alloyaudioplayer.Player.PlaylistManager.audioQueue
import com.emmett222.alloyaudioplayer.Settings.SettingsChange
import com.emmett222.alloyaudioplayer.Settings.VisualizerType
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import com.emmett222.alloyaudioplayer.Util.FileUtil
import com.emmett222.alloyaudioplayer.Util.StringUtil

/**
 * Player screen for Alloy Audio Player. Must be called with
 *
 * @author Emmett Grebe
 * @version 8-16-2026
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        // These are companion callbacks.
        var onFileChangeListener: ((File) -> Unit)? = null

        const val FAST_DURATION_MS = 60000
    }

    /**
     * vvvvv ---------- Player ---------- vvvvv
     */
    lateinit var controller: MediaController
    var vis: Visualizer? = null // Nullable for later safety check.
    var visType = 5
    lateinit var intentFile: File

    /**
     * vvvvv ---------- Status ---------- vvvvv
     */
    var isOld: Boolean = false
    var isStart: Boolean = true
    var shortenTitles: Boolean = false
    var inMenu: Boolean = false

    /**
     * vvvvv ---------- Graphics ---------- vvvvv
     */
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updater: Runnable
    private lateinit var titleString: TextView
    private lateinit var currentTime: TextView
    private lateinit var endTime: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var visualizerView: BaseGraphic
    private lateinit var menuGraphic: ConstraintLayout
    private lateinit var menuRecycler: RecyclerView
    private lateinit var menuVisRecycler: RecyclerView
    private lateinit var menuQueueRecycler: RecyclerView
    private lateinit var menuFilesRecycler: RecyclerView
    private var color1: Int = -1
    private var color2: Int = -1
    private var color3: Int = -1


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
        this.isOld = intent.getStringExtra(FileListActivity.ISOLD_DATA) == "true"

        if (isOld) {
            intentFile = MediaEngine.getCurrentFile()
            PlaylistManager.setupFiles(intentFile)
        } else {
            val pathString = intent.getStringExtra(FileListActivity.PATH_DATA)
            if (pathString != null) {
                intentFile = File(pathString)
                PlaylistManager.setupFiles(intentFile)
            } else {
                finish()
                return
            }
        }

        titleString = findViewById(R.id.titleString)
        currentTime = findViewById(R.id.currentNum)
        endTime = findViewById(R.id.endNum)
        seekBar = findViewById(R.id.timeSeekBar)
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

            readSettings()
            setupGestures()
            setupFastBtns()
            setupMenuBtn()
            setupShuffleBtn()
            setupSkipBtns()
            setupPauseBtn()
            setupRepeatOneBtn()
            setupRepeatPlaylistBtn()
            setupVisualizer()

            PlaylistManager.playNewSong(intentFile)

        }, ContextCompat.getMainExecutor(this))

        isStart = false;
    }

    /**
     * Runs when the player is not visible. If music is playing, this will stop the updater from
     * trying to keep updating the time.
     */
    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updater)
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

        val newPath = intent.getStringExtra(FileListActivity.PATH_DATA)
        if (newPath != null) {
            val newFile = File(newPath)

            // Only interrupt playback if it's actually a new song
            if (PlaylistManager.audioFile.absolutePath != newFile.absolutePath) {
                PlaylistManager.playNewSong(newFile)
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
     * Reads the settings from the preferences and makes changes depending on their output.
     */
    private fun readSettings() {
        visType = SettingsChange.getVisType(this)
        PlaylistManager.repeatPlaylistOn = SettingsChange.getRepeatMode(this)
        PlaylistManager.shuffleOn = SettingsChange.getShufMode(this)
        shortenTitles = SettingsChange.getShortMode(this)
        color1 = SettingsChange.getColor1(this)
        color2 = SettingsChange.getColor2(this)
        color3 = SettingsChange.getColor3(this)

        if (PlaylistManager.repeatPlaylistOn) {
            findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.btn_repeatplayliston)
        }
        if (PlaylistManager.shuffleOn) {
            findViewById<ImageButton>(R.id.shuffleBtn).setImageResource(R.drawable.btn_shuffleon)
            PlaylistManager.shuffle()
        }
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
        findViewById<View>(R.id.main).setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)
            return true
        })

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
     * Sets up the audio visualizer.
     */
    @OptIn(UnstableApi::class)
    private fun setupVisualizer() {
        visualizerView.changeScreen(visType)

        var currentActiveSessionId = -1

        controller.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = controller.sessionExtras.getInt("AUDIO_SESSION_ID", 0)

                    // Only run with valid ID and it's from the running one.
                    // This is so it doesn't crash when user seek ahead.
                    if (sessionId > 0 && sessionId != currentActiveSessionId) {
                        currentActiveSessionId = sessionId

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
                                // Unused for now
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
                    } else if (sessionId > 0) {
                        vis?.enabled = true
                    }
                } else {
                    vis?.enabled = false
                }
            }
        })
    }

    /**
     * Helper method to set up the time views on load.
     */
    private fun setupTime() {
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
                        endTime.text = StringUtil.formatMinutesAndSeconds(duration)

                        // Start the UI updater loop now that we have a max
                        handler.post(updater)
                    }
                    Player.STATE_ENDED -> {
                        handler.removeCallbacks(updater)
                        controller.seekToNext()
                        makeQueueMenu(audioQueue)
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

        arrayOf(currentTime, endTime).forEach {
            it.setTextColor(color1)
            it.setBackgroundColor(color2)
        }
    }

    /**
     * Helper method to set up the pause button on load.
     */
    private fun setupPauseBtn() {
        val playBtn: ImageButton = findViewById(R.id.playBtn)
        playBtn.setOnClickListener {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        if (isOld && !MediaEngine.getPaused()) {
            controller.pause()
            playBtn.setImageResource(R.drawable.btn_play)
            handler.removeCallbacks(updater)
        }

        val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (!isPlaying) {
                    playBtn.setImageResource(R.drawable.btn_pause)
                    handler.removeCallbacks(updater)
                } else {
                    playBtn.setImageResource(R.drawable.btn_play)
                    handler.post(updater)
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val newTitle = mediaItem.mediaMetadata.title.toString()
                    setupTitle(newTitle)
                    makeQueueMenu(PlaylistManager.audioQueue)
                }
            }
        }
        controller.addListener(playerListener)
    }

    /**
     * Helper method to set up the fast-forward and fast rewind buttons.
     */
    private fun setupFastBtns() {
        val ffBtn: ImageButton = findViewById(R.id.fastForward)
        val frBtn: ImageButton = findViewById(R.id.fastRewind)

        ffBtn.setOnClickListener {
            // Go forward 1 minute.
            fastForward(FAST_DURATION_MS)
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        frBtn.setOnClickListener {
            // Rewind 1 minute.
            fastRewind(FAST_DURATION_MS)
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Skips ahead in the song.
     *
     * @param duration The time to skip in ms
     */
    fun fastForward(duration: Int) {
        controller.seekTo(controller.currentPosition + FAST_DURATION_MS)
    }

    /**
     * Skips backwards in the song.
     *
     * @param duration The time to skip in ms
     */
    fun fastRewind(duration: Int) {
        controller.seekTo(controller.currentPosition - FAST_DURATION_MS )
    }

    private fun setupSkipBtns() {
        val skipFBtn: ImageButton = findViewById(R.id.skipForwardBtn)
        val skipBBtn: ImageButton = findViewById(R.id.skipBackBtn)

        skipFBtn.setOnClickListener {
            controller.seekToNext()
            makeQueueMenu(audioQueue)
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
                PlaylistManager.repeatOneOn = false;
            } else {
                controller.repeatMode = Player.REPEAT_MODE_ONE;
                repeatOneBtn.setImageResource(R.drawable.btn_repeat1on)
                PlaylistManager.repeatOneOn = true;
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
            if (PlaylistManager.shuffleOn) {
                shuffleBtn.setImageResource(R.drawable.btn_shuffleoff)
                PlaylistManager.unshuffle()
                PlaylistManager.shuffleOn = false;
            } else {
                shuffleBtn.setImageResource(R.drawable.btn_shuffleon)
                PlaylistManager.shuffle()
                PlaylistManager.shuffleOn = true;
            }
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
        visGraphic.background = color2.toDrawable()

        arrayOf(menuRecycler, menuVisRecycler, menuQueueRecycler, menuFilesRecycler).forEach {
            it.layoutManager = LinearLayoutManager(applicationContext)
            ColorUtil.updateAllTextColors(it, color1)
            it.setBackgroundColor(color2)
            ColorUtil.updateAllAccentColors(it, color3)
        }

        makeVisMenu()
        makeFilesMenu(PlaylistManager.audioFile.parentFile?.parentFile, PlaylistManager.audioFile.parentFile)

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
            val foundType = VisualizerType.entries.find { it.label == clickedItem }?.id ?: 1
            visualizerView.changeScreen(foundType)

            SettingsChange.saveVisType(this, foundType)

            if (clickedItem == VisualizerType.NOVIS.label) {
                visualizerView.setBackgroundColor(ColorUtil.darkenColor(color2.toDrawable(), 128))
            } else {
                visualizerView.setBackgroundColor(color2)
            }
            backToVis(visualizerView, menuGraphic)
        }
    }

    /**
     * Makes a new files menu.
     */
    private fun makeFilesMenu(backOption: File?, folder: File?) {
        if (backOption == null || folder == null) return
        val rawFiles = folder.listFiles() ?: return
        val filteredFiles: Array<File> = FileUtil.filterFiles(rawFiles, this)
        // Whenever an item is clicked on the files menu, the start menu callback send the info
        // back to here. Uses the static global variables in the companion to determine which was
        // clicked.
        menuFilesRecycler.adapter = FilesMenuAdapter(this, backOption, filteredFiles)
        { clickedItem, isGoTo ->
            if (clickedItem.isDirectory) { // Folder.
                makeFilesMenu(clickedItem.parentFile, clickedItem)
            } else {
                if (isGoTo) {
                    PlaylistManager.playNewSong(clickedItem)
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
        masterList.add(QueueRowItem(PlaylistManager.audioFile, true, false))

        // Add the queue tracks
        queueItems.forEach { file ->
            masterList.add(QueueRowItem(file, false, true))
        }

        // Add the rest of the playlist tracks
        val nextIndex = PlaylistManager.currentPosition + 1
        if (nextIndex < PlaylistManager.allFiles.size) {
            for (i in nextIndex until PlaylistManager.allFiles.size) {
                masterList.add(QueueRowItem(PlaylistManager.allFiles[i],
                    isCurrentPlaying = false,
                    isInQueue = false
                ))
            }
        }

        menuQueueRecycler.adapter = QueueAdapter(
            this, masterList,
            onItemClick = { clickedItem ->
                    PlaylistManager.playNewSong(clickedItem)
                    makeQueueMenu(queueItems)
                },
            onQueueClick = { clickedItem ->
                queueItems.addLast(clickedItem)
                makeQueueMenu(queueItems)
            },
            onRemoveClick = { clickedItem, isInQueue ->
                PlaylistManager.remove(clickedItem, isInQueue) },
            onItemMove = { finalModelList ->
                PlaylistManager.changeList(finalModelList)

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
            if (PlaylistManager.repeatPlaylistOn) {
                repeatPlaylistBtn.setImageResource(R.drawable.btn_repeatplaylistoff)
                PlaylistManager.repeatPlaylistOn = false;
            } else {
                repeatPlaylistBtn.setImageResource(R.drawable.btn_repeatplayliston)
                PlaylistManager.repeatPlaylistOn = true;
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Helper method to set up scrolling title on load.
     */
    private fun setupTitle(title: String) {
        if (shortenTitles) {
            titleString.text = NameUtil.removeDescriptors(title)
        } else {
            titleString.text = title
        }

        titleString.postDelayed({ // Only fires when the title is loaded.
            titleString.isSelected = true // So the marquee starts on load,
        }, 2000) // But waits two seconds before moving.

        titleString.setTextColor(color1)
        titleString.setBackgroundColor(color2)
    }

    /**
     * Skips backwards one song.
     * If song is more than 10 seconds in, start current song over. If not:
     * If repeat playlist is on and the player is on the first song, skip backwards will take player
     * to last song. If not, it does not do this.
     */
    fun skipBackward() {
        if (controller.currentPosition > 10000) { // 10 Seconds
            controller.seekTo(0) // Go back to beginning.
            return
        }
        controller.seekToPrevious()
        makeQueueMenu(audioQueue)
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
        currentTime.text = StringUtil.formatMinutesAndSeconds(m)
    }
}