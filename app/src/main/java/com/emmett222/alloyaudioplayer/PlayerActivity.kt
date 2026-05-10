package com.emmett222.alloyaudioplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class PlayerActivity : AppCompatActivity() {

    var audioFile: File? = null

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

        var titleString: TextView = findViewById(R.id.titleString)
        titleString.text = audioFile?.name
    }

    companion object {
        var mediaPlayer: MediaPlayer? = null

        // Call this function when a file is tapped
        fun playAudio(file: File) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.path)
                prepare()
                start()
            }
        }
    }
}