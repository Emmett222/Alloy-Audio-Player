package com.emmett222.alloyaudioplayer

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class PlayerActivity : AppCompatActivity() {

    var audioFile: File? = null
    var isStart: Boolean = true;

    companion object {
        var mediaPlayer: MediaPlayer = MediaPlayer()

        /**
         * Plays the music.
         */
        fun play(file: File?) {
            mediaPlayer.let {
                if (!it.isPlaying) {
                    mediaPlayer.apply {
                        setDataSource(file?.path)
                        prepare()
                        start()
                    }
                }
            }
        }

        /**
         * Pauses the music.
         */
        fun pause() {
            mediaPlayer.pause()
        }

        /**
         * Unpauses the music.
         */
        fun unPause() {
            mediaPlayer.start()
        }
    }

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

        var titleString: TextView = findViewById(R.id.titleString)
        titleString.text = audioFile?.name

        var playBtn: ImageButton = findViewById(R.id.playBtn)

        playBtn.setOnClickListener {
            if (PlayerActivity.mediaPlayer?.isPlaying == true) {
                PlayerActivity.pause()
                playBtn.setImageResource(R.drawable.play_arrow_24px)
            } else {
                PlayerActivity.unPause()
                playBtn.setImageResource(R.drawable.pause_24px)
            }
        }

        PlayerActivity.play(audioFile)
        isStart = false;
    }



}