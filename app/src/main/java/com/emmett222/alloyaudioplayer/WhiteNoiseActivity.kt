package com.emmett222.alloyaudioplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.emmett222.alloyaudioplayer.Background.WhiteNoiseEngine

/**
 * White noise activity for Alloy Audio Player. Gives the user options to create their own white
 * noise, or pick from predetermined ones. The predetermined noises will be commonly found noises
 * like white noise, brown noise, green noise, etc.
 * Also has a timer to stop after a certain amount of time.
 *
 * @author Emmett Grebe
 * @version 5-27-2026
 */
class WhiteNoiseActivity : AppCompatActivity() {

    private var noiseEngine: WhiteNoiseEngine? = null
    private var isBound = false
    private var isPlaying: Boolean = true

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // Cast the IBinder and get the instance
            val binder = service as WhiteNoiseEngine.LocalBinder
            noiseEngine = binder.getEngine()
            isBound = true
            noiseEngine?.playNoise()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            noiseEngine = null
            isBound = false
        }
    }

    /**
     * Runs on the activity appearing.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_wn)

        Intent(this, WhiteNoiseEngine::class.java).also { intent ->
            // BIND_AUTO_CREATE tells Android to spin up the service if it isn't already running.
            bindService(intent, connection, android.content.Context.BIND_AUTO_CREATE)
        }

        setupSeekBars()
        setupPauseBtn()
    }

    private fun setupSeekBars() {
        val decBar: SeekBar = findViewById(R.id.decimationSeekBar)
        val filBar: SeekBar = findViewById(R.id.filtrationSeekBar)
        val modBar: SeekBar = findViewById(R.id.modulationSeekBar)
        val spaBar: SeekBar = findViewById(R.id.spatialSeekBar)
        val basBar: SeekBar = findViewById(R.id.bassCutSeekBar)
        val winBar: SeekBar = findViewById(R.id.windSeekBar)

        // OnSeekBarChangeListener is like an interface. If you want to listen to seekbar, you must
        // do all 3 methods.
        decBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateWarmth(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        filBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateTexture(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        modBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateModulation(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        spaBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateSpatialWidth(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        basBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateBassCut(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        winBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // Called whenever the seekbar is changed.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // ONLY seek if the user touched it, not the system
                    noiseEngine?.updateWindGusting(progress)
                }
            }

            // Unused methods.
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Sets up the pause button. Pausing stops white noise, and playing starts white noise back up.
     * Also makes the button change it's graphic when pressed. Lit up for paused, dimmed for
     * playing.
     */
    private fun setupPauseBtn() {
        val playBtn: ImageButton = findViewById(R.id.pauseBtn)
        playBtn.setOnClickListener {
            if (isPlaying) {
                noiseEngine?.pauseNoise()
                playBtn.setImageResource(R.drawable.btn_play)
                isPlaying = false
            } else {
                noiseEngine?.playNoise()
                playBtn.setImageResource(R.drawable.btn_pause)
                isPlaying = true
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}