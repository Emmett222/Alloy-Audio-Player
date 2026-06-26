package com.emmett222.alloyaudioplayer

import android.content.Intent
import com.emmett222.alloyaudioplayer.MyAdapter
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Background.MediaEngine
import com.emmett222.alloyaudioplayer.Player.PlayerActivity
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import java.util.jar.Attributes

class FileListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noFilesText: TextView
    private lateinit var folderNameText: TextView
    private lateinit var songTitleText: TextView
    private lateinit var currentFolder: File
    private lateinit var initialRootFolder: File

    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_file_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recycler_view)
        noFilesText = findViewById(R.id.nofiles_textview)
        folderNameText = findViewById(R.id.folderName)
        songTitleText = findViewById(R.id.currSongTitle)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val path: String = intent.getStringExtra("path") ?: ""
        currentFolder = File(path)
        initialRootFolder = File(path)

        setupBtns()

        loadDirectory(currentFolder)
    }

    private fun loadDirectory(folder: File) {
        folderNameText.text = if (folder.name.isEmpty()) "Root" else folder.name

        val rawFiles: Array<File>? = folder.listFiles()
        val filteredFiles: Array<File>? = rawFiles?.filter { file ->
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
        }?.toTypedArray()

        if (filteredFiles == null || filteredFiles.isEmpty()) {
            noFilesText.visibility = View.VISIBLE
            recyclerView.adapter = null // Clear old visible elements from the list frame
            return
        }
        noFilesText.visibility = View.INVISIBLE

        val animationController = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_left)
        recyclerView.layoutAnimation = animationController

        // Instantiate a fresh adapter binding instance with the explicit callback logic block
        recyclerView.adapter = MyAdapter(this, filteredFiles) { clickedFile ->
            if (clickedFile.isDirectory) {
                currentFolder = clickedFile
                loadDirectory(currentFolder)
            } else {
                PlayerActivity.onFileChangeListener = { activeTrack ->
                    songTitleText.text = NameUtil.removeDescriptors(activeTrack.name)
                }

                // If it's a song asset, execute standard audio media engine boot playback routines
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("path", clickedFile.absolutePath)
                }
                startActivity(intent)
            }
        }
    }

    /**
     * Helper function to set up the two buttons at the top, and the currently playing song.
     */
    private fun setupBtns() {
        val backBtn: ImageButton = findViewById(R.id.imageBtn)
        val infoBtn: ImageButton = findViewById(R.id.infoBtn)

        backBtn.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

            // If we are at the very beginning or can't go up, close the screen
            if (currentFolder == initialRootFolder || currentFolder.parentFile == null) {
                finish()
            } else {
                // Step back up one level in the folder structure tree
                currentFolder = currentFolder.parentFile!!
                loadDirectory(currentFolder)
            }
        }

        infoBtn.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }

        songTitleText.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            startActivity(intent)
        }
    }
}