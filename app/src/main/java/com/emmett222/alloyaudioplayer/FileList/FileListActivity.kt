package com.emmett222.alloyaudioplayer.FileList

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Player.PlayerActivity
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Settings.AnimationType
import com.emmett222.alloyaudioplayer.Settings.SettingsChange
import com.emmett222.alloyaudioplayer.Util.FileUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import kotlin.math.abs

/**
 * Lists the files. Only shows audio files.
 *
 * @author Emmett Grebe
 * @version 8-21-2026
 */
class FileListActivity : AppCompatActivity() {
    companion object {
        const val PATH_DATA = "path"
        const val ISOLD_DATA = "isOld"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PaginatedAdapter
    private lateinit var noFilesText: TextView
    private lateinit var folderNameText: TextView
    private lateinit var pgnteText: TextView
    private lateinit var songTitleText: TextView
    private lateinit var currentFolder: File
    private lateinit var initialRootFolder: File

    private var shortenTitles: Boolean = false
    private var doPgnte: Boolean = false
    private var pgnteAmount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_file_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // This replaces the default back button functionality.
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        // This adds the custom call back to the dispatcher. The dispatcher is responsible for
        // handling the back click.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        recyclerView = findViewById(R.id.recycler_view)
        noFilesText = findViewById(R.id.nofiles_textview)
        folderNameText = findViewById(R.id.folderName)
        pgnteText = findViewById(R.id.pgnteNumber)
        songTitleText = findViewById(R.id.currSongTitle)

        shortenTitles = SettingsChange.getShortMode(this)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val path: String = SettingsChange.getDefaultFolder(this)
        currentFolder = File(path)
        initialRootFolder = File(path)

        pgnteAmount = SettingsChange.getPaginateNum(this)
        if (pgnteAmount > 0) {
            doPgnte = true
        }

        setupGestures()
        loadDirectory(currentFolder)
        setupBtns()
        changeColors()
    }

    private fun loadDirectory(folder: File) {
        folderNameText.text = if (folder.name.isEmpty()) "" else folder.name

        val rawFiles: Array<File>? = folder.listFiles()
        val filteredFiles: Array<Array<File>> =
            FileUtil.paginate(FileUtil.filterFiles(rawFiles, this), pgnteAmount)

        if (filteredFiles.isEmpty()) {
            noFilesText.visibility = View.VISIBLE
            recyclerView.adapter = null // Clear old visible elements from the list frame
            return
        }
        noFilesText.visibility = View.INVISIBLE

        when (SettingsChange.getAnimType(this)) {
            AnimationType.LEFT.id -> {
                val animationController =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_left)
                recyclerView.layoutAnimation = animationController
            }

            AnimationType.RIGHT.id -> {
                val animationController =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_right)
                recyclerView.layoutAnimation = animationController
            }
        }

        // Instantiate a fresh adapter binding instance with the explicit callback logic block
        adapter = PaginatedAdapter(this, filteredFiles) { clickedFile ->
            if (clickedFile.isDirectory) {
                currentFolder = clickedFile
                loadDirectory(currentFolder)
            } else {
                PlayerActivity.Companion.onFileChangeListener = { activeTrack ->
                    if (shortenTitles) {
                        songTitleText.text = NameUtil.removeDescriptors(activeTrack.name)
                    } else {
                        songTitleText.text = activeTrack.name
                    }
                }

                // If it's a song asset, execute standard audio media engine boot playback routines
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(PATH_DATA, clickedFile.absolutePath)
                    putExtra(ISOLD_DATA, "false")
                }
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter
        pgnteText.text = adapter.getCurrPage()
    }

    /**
     * Sets up custom gestures for navigating the pages.
     */
    private fun setupGestures() {
        // The 'object : ' syntax is used to create an anonymous class. It is a one time object that
        // implements an interface or extends a class, without needing to create a new .kt file.
        // Think of it as "Create an (object) that acts like (:) this class/interface (____) and
        // let me customize it.
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
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
                    // Swipe from left to right quickly.
                    if ((e1.x < 300) && abs(diffX) > abs(diffY)) { // Left of screen right
                        if ((diffX > 150) && (abs(velocityX) > 150)) { // Far enough and fast enough.
                            pgntePrev()
                        }
                    }

                    // Motion determining.
                    // Swipe from right to left quickly.
                    if ((e1.x > 300) && abs(diffX) < abs(diffY)) { // Right of screen left
                        if ((diffX > 150) && (abs(velocityX) > 150)) { // Far enough and fast enough.
                            pgnteNext()
                        }
                    }

                    return false
                }
            })

        // Intercept touches on the root view
        // '_' represents the View parameter. Underscore is used to safely ignore it, since it is
        // not needed.
        findViewById<View>(R.id.main).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    /**
     * Helper function to set up the two buttons at the top, and the currently playing song.
     */
    private fun setupBtns() {
        val backBtn: ImageButton = findViewById(R.id.imageBtn)
        val infoBtn: ImageButton = findViewById(R.id.infoBtn)

        backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            goBack()
        }

        infoBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        songTitleText.setOnClickListener {
            if (!songTitleText.text.equals("")) {
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(ISOLD_DATA, "true")
                }
                startActivity(intent)
            }
        }

        // Pagination bar
        if (doPgnte) {
            findViewById<LinearLayout>(R.id.pgnteContainer).visibility = View.VISIBLE
            val pgntePrev: ImageButton = findViewById(R.id.pgntePrev)
            val pgnteNext: ImageButton = findViewById(R.id.pgnteNext)

            pgnteText.text = adapter.getCurrPage()

            pgntePrev.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pgntePrev()
            }
            pgnteNext.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pgnteNext()
            }
        }
    }

    /**
     * Goes back one level of the file system. If there is nowhere else to go, like the root folder,
     * it closes the activity.
     */
    private fun goBack() {
        // If we are at the very beginning or can't go up, close the screen
        if (currentFolder == initialRootFolder || currentFolder.parentFile == null) {
            finish()
        } else {
            // Step back up one level in the folder structure tree
            currentFolder = currentFolder.parentFile!!
            loadDirectory(currentFolder)
        }
    }

    /**
     * Goes backwards a page, animates, then changes the text accordingly.
     */
    private fun pgntePrev() {
        adapter.previousPage()
        recyclerView.scheduleLayoutAnimation()
        pgnteText.text = adapter.getCurrPage()
    }

    /**
     * Goes forwards a page, animates, then changes the text accordingly.
     */
    private fun pgnteNext() {
        adapter.nextPage()
        recyclerView.scheduleLayoutAnimation()
        pgnteText.text = adapter.getCurrPage()
    }

    /**
     * Changes the colors based on user's settings. Applies default colors if not.
     */
    private fun changeColors() {
        val color1 = SettingsChange.getColor1(this)
        val color2 = SettingsChange.getColor2(this)

        arrayOf<TextView>(findViewById(R.id.folderName), findViewById(R.id.currSongTitle)).forEach {
            it.setTextColor(color1)
            it.setBackgroundColor(color2)
        }
    }

}