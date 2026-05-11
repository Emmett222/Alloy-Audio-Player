package com.emmett222.alloyaudioplayer

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

/**
 * Player screen for Alloy Audio Player.
 *
 * @author Emmett Grebe
 * @version 5-11-2026
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var audioFile: File
    var isStart: Boolean = true;
    var repeatOn: Boolean = false;

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updater: Runnable

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
            // Regular seekTo jumps back a few seconds. This makes it go to the right time.
            mediaPlayer.seekTo(time.toLong(), MediaPlayer.SEEK_CLOSEST)
        }

        fun turnOnRepeat() {
            mediaPlayer.isLooping = true
        }

        fun turnOffRepeat() {
            mediaPlayer.isLooping = false
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

        // All the setups.
        setupTitle()
        setupTime()
        setupPauseBtn()
        setupFastBtns()
        setupRepeatBtn()

        isStart = false;
    }

    /**
     * Helper method to setup the time views on load.
     */
    private fun setupTime() {
        var duration: Int = PlayerActivity.mediaPlayer.duration
        var endText: TextView = findViewById(R.id.endNum)
        var seekBar: SeekBar = findViewById(R.id.timeSeekBar)
        seekBar.min = 0
        seekBar.max = duration
        endText.text = formatMinutesAndSeconds(duration)
        updater = Runnable { // A runnable is just a group of code waiting to be executed.
            seekBar.progress = PlayerActivity.mediaPlayer.currentPosition

            changeTime(PlayerActivity.mediaPlayer.currentPosition)

            handler.postDelayed(updater, 200) // Makes it loop.
        }
        handler.post(updater)

        // OnSeekBarChangeListener is like an interface. If you want to listen to seekbar, you must
        // do all 3 methods.
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    PlayerActivity.seek(progress)
                    changeTime(progress)
                }
            }
            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // This is to fix the timer going further than the song length.
        // It fires when the song is over.
        PlayerActivity.mediaPlayer.setOnCompletionListener {
            handler.removeCallbacks(updater)
            seekBar.progress = seekBar.max
        }
    }

    /**
     * Helper method to setup the pause button on load.
     */
    private fun setupPauseBtn() {
        var playBtn: ImageButton = findViewById(R.id.playBtn)
        playBtn.setOnClickListener {
            if (PlayerActivity.mediaPlayer.isPlaying == true) {
                PlayerActivity.pause()
                playBtn.setImageResource(R.drawable.play_arrow_24px)
                handler.removeCallbacks(updater)
            } else {
                PlayerActivity.unPause()
                playBtn.setImageResource(R.drawable.pause_24px)
                handler.post(updater)
            }
        }
    }

    /**
     * Helper method to setup the fast forward and fast rewind buttons.
     */
    private fun setupFastBtns() {
        var ffBtn: ImageButton = findViewById(R.id.fastForward)
        var frBtn: ImageButton = findViewById(R.id.fastRewind)

        ffBtn.setOnClickListener {
            // Go forward 1 minute.
            seek(PlayerActivity.mediaPlayer.currentPosition + 60000)
        }

        frBtn.setOnClickListener {
            // Rewind 1 minute.
            seek(PlayerActivity.mediaPlayer.currentPosition - 60000)
        }
    }

    /**
     * Helper method to setup the repeat button on load.
     */
    private fun setupRepeatBtn() {
        var repeatBtn: ImageButton = findViewById(R.id.repeatBtn)
        PlayerActivity.turnOffRepeat()
        repeatBtn.setOnClickListener {
            if (repeatOn) {
                PlayerActivity.turnOffRepeat()
                repeatBtn.backgroundTintList = ColorStateList.valueOf(Color.RED)
                repeatOn = false;
            } else {
                PlayerActivity.turnOnRepeat()
                repeatBtn.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
                repeatOn = true;
            }
        }
    }

    /**
     * Helper method to setup scrolling title on load.
     */
    private fun setupTitle() {
        var titleString: TextView = findViewById(R.id.titleString)
        titleString.text = audioFile.name

        titleString.postDelayed({ // Only fires when the title is loaded.
            titleString.isSelected = true // So the marquee starts on load,
        }, 2000) // But waits two seconds before moving.
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