package com.emmett222.alloyaudioplayer

import com.emmett222.alloyaudioplayer.MyAdapter
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileListActivity : AppCompatActivity() {
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

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val noFilesText: TextView = findViewById(R.id.nofiles_textview)

        val path: String? = intent.getStringExtra("path")

        val root: File = File(path)
        val backBtn: ImageButton = findViewById(R.id.imageBtn)
        val folderNameText: TextView = findViewById(R.id.folderName)
        val infoBtn: ImageButton = findViewById(R.id.infoBtn)

        if (path != null) {
            folderNameText.text = root.name
        } else {
            folderNameText.text = "Root"
        }

        backBtn.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            finish()
        }

        infoBtn.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }


        val rawFiles: Array<File>? = root.listFiles()
        val files: Array<File>? = rawFiles?.filter { file ->
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

        if (files == null || files.size == 0) {
            noFilesText.visibility = View.VISIBLE
            return;
        }

        noFilesText.visibility = View.INVISIBLE

        recyclerView.setLayoutManager(LinearLayoutManager(this@FileListActivity))
        recyclerView.setAdapter(MyAdapter(this, files))
    }
}