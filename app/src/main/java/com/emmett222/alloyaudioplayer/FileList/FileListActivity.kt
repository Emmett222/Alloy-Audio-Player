package com.emmett222.alloyaudioplayer.FileList

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.emmett222.alloyaudioplayer.Player.PlayerActivity
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Settings.AnimationType
import com.emmett222.alloyaudioplayer.Settings.SettingsChange
import com.emmett222.alloyaudioplayer.Util.FileUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import com.emmett222.alloyaudioplayer.databinding.ActivityFileListBinding
import java.io.File

/**
 * Lists the files. Only shows audio files.
 *
 * @author Emmett Grebe
 * @version 8-26-2026
 */
class FileListActivity : AppCompatActivity() {
    companion object {
        const val PATH_DATA = "path"
        const val ISOLD_DATA = "isOld"
    }

    private lateinit var binding: ActivityFileListBinding
    private lateinit var viewConfig: ViewConfiguration
    private lateinit var adapter: PaginatedAdapter
    private lateinit var currentFolder: File
    private lateinit var initialRootFolder: File

    private var shortenTitles: Boolean = false
    private var doPaginate: Boolean = false
    private var paginateAmount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_file_list)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
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

        binding = ActivityFileListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shortenTitles = SettingsChange.getShortMode(this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewConfig = ViewConfiguration.get(this)

        val path: String = SettingsChange.getDefaultFolder(this)
        currentFolder = File(path)
        initialRootFolder = File(path)

        paginateAmount = SettingsChange.getPaginateNum(this)
        if (paginateAmount > 0) {
            doPaginate = true
        }

        loadDirectory(currentFolder)
        setupBtns()
        changeColors()
    }

    private fun loadDirectory(folder: File) {
        binding.folderName.text = if (folder.name.isEmpty()) "" else folder.name

        val rawFiles: Array<File>? = folder.listFiles()
        val filteredFiles: Array<Array<File>> =
            FileUtil.paginate(FileUtil.filterFiles(rawFiles, this), paginateAmount)

        if (filteredFiles.isEmpty()) {
            binding.nofilesTextview.visibility = View.VISIBLE
            binding.recyclerView.adapter = null // Clear old visible elements from the list frame
            return
        }
        binding.nofilesTextview.visibility = View.INVISIBLE

        when (SettingsChange.getAnimType(this)) {
            AnimationType.LEFT.id -> {
                val animationController =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_left)
                binding.recyclerView.layoutAnimation = animationController
            }

            AnimationType.RIGHT.id -> {
                val animationController =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_right)
                binding.recyclerView.layoutAnimation = animationController
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
                        binding.currSongTitle.text = NameUtil.removeDescriptors(activeTrack.name)
                    } else {
                        binding.currSongTitle.text = activeTrack.name
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
        binding.recyclerView.adapter = adapter
        binding.paginateNumber.text = adapter.getCurrPage()
    }

    /**
     * Helper function to set up the two buttons at the top, and the currently playing song.
     */
    private fun setupBtns() {
        val backBtn: ImageButton = binding.imageBtn
        val infoBtn: ImageButton = binding.infoBtn

        backBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            goBack()
        }

        infoBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        binding.currSongTitle.setOnClickListener {
            if (!binding.currSongTitle.text.equals("")) {
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(ISOLD_DATA, "true")
                }
                startActivity(intent)
            }
        }

        // Pagination bar
        if (doPaginate) {
            findViewById<LinearLayout>(R.id.paginateContainer).visibility = View.VISIBLE
            val paginatePrev: ImageButton = binding.paginatePrev
            val paginateNext: ImageButton = binding.paginateNext

            binding.paginateNumber.text = adapter.getCurrPage()

            paginatePrev.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                paginatePrev()
            }
            paginateNext.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                paginateNext()
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
    private fun paginatePrev() {
        adapter.previousPage()
        binding.recyclerView.scheduleLayoutAnimation()
        binding.paginateNumber.text = adapter.getCurrPage()
    }

    /**
     * Goes forwards a page, animates, then changes the text accordingly.
     */
    private fun paginateNext() {
        adapter.nextPage()
        binding.recyclerView.scheduleLayoutAnimation()
        binding.paginateNumber.text = adapter.getCurrPage()
    }

    /**
     * Changes the colors based on user's settings. Applies default colors if not.
     */
    private fun changeColors() {
        val color1 = SettingsChange.getColor1(this)
        val color2 = SettingsChange.getColor2(this)

        arrayOf(binding.folderName, binding.currSongTitle).forEach {
            it.setTextColor(color1)
            it.setBackgroundColor(color2)
        }
    }

}