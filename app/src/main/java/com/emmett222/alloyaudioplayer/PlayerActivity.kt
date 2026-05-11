package com.emmett222.alloyaudioplayer

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.emmett222.alloyaudioplayer.PlayerActivity
import org.w3c.dom.Text
import java.io.File

class PlayerActivity : AppCompatActivity() {

    var audioFile: File? = null
    var isStart: Boolean = true;

    companion object {
        var mediaPlayer: MediaPlayer = MediaPlayer()

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

        fun pause() {
            mediaPlayer.pause()
        }

        fun unPause() {
            mediaPlayer.start()
        }

        fun seek(time: Int) {
            mediaPlayer.seekTo(time)
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
        PlayerActivity.play(audioFile)

        var titleString: TextView = findViewById(R.id.titleString)
        titleString.text = audioFile?.name

        titleString.postDelayed({
            titleString.isSelected = true // So the marquee starts on load,
        }, 2000) // But waits two seconds before moving.

        // Time stuff
        var duration: Int = PlayerActivity.mediaPlayer.duration
        var timeSeekBar: SeekBar = findViewById(R.id.timeSeekBar)
        var endText: TextView = findViewById(R.id.endNum)
        timeSeekBar.min = 0
        timeSeekBar.max = duration
        changeTime(0)
        endText.text = formatMinutesAndSeconds(duration)

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

        var seekBar: SeekBar = findViewById(R.id.timeSeekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    PlayerActivity.seek(progress)
                    changeTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })



        isStart = false;
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
     *
     */
    fun formatMinutesAndSeconds(m: Int): String {
        val minutes = (m / 1000) / 60
        val seconds = (m / 1000) % 60

        return String.format("%d:%02d", minutes, seconds)
    }
}