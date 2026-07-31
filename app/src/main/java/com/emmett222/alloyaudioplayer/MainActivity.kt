package com.emmett222.alloyaudioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.emmett222.alloyaudioplayer.Settings.SettingsActivity
import com.emmett222.alloyaudioplayer.Settings.SettingsChange
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import kotlin.arrayOf

/**
 * Opening screen for Alloy. Asks for permissions if needed and takes user to file screen.
 *
 * @author Emmett Grebe
 * @version 7-31-2026
 */
class MainActivity : AppCompatActivity() {

    // Make the callback before the activity is even started.
    // Also, must register launchers before or during onCreate.
    // Use it by calling .launch().
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (audioGranted && recordGranted) {
            openFileList()
        } else {
            Toast.makeText(
                this,
                "Both storage and audio permissions are required!",
                Toast.LENGTH_SHORT
            ).show()
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

        setContentView(R.layout.activity_main)

        val filesBtn: ImageButton = findViewById(R.id.filesBtn)
        val wnBtn: ImageButton = findViewById(R.id.wnBtn)
        val settingsBtn: ImageButton = findViewById(R.id.settingsBtn)

        filesBtn.setOnClickListener {
            if (checkPermission()) {
                openFileList()
            } else {
                requestPermission()
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        wnBtn.setOnClickListener {
            if (checkPermission()) {
                startActivity(Intent(this@MainActivity, WhiteNoiseActivity::class.java))
            } else {
                requestPermission()
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        settingsBtn.setOnClickListener {
            if (checkPermission()) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            } else {
                requestPermission()
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        changeColors()
    }

    /**
     * Runs when the activity is resumed. After settings is closed and the user is taken back to
     * the main menu, this changes the colors based on if the user altered them.
     */
    override fun onResume() {
        super.onResume()

        val color1 = SettingsChange.getColor1(this)
        val color2 = SettingsChange.getColor2(this)
        val color3 = SettingsChange.getColor3(this)

        val mainLayout = findViewById<View>(R.id.main)
        mainLayout.setBackgroundColor(color2)

        ColorUtil.updateAllTextColors(mainLayout, color1)
        ColorUtil.updateAllAccentColors(mainLayout, color3)
    }

    /**
     * Checks if the permissions are granted.
     * @return True if READ_MEDIA_AUDIO and RECORD_AUDIO are granted, false otherwise.
     */
    private fun checkPermission(): Boolean {
        val rmaResult = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val raResult = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return rmaResult && raResult
    }

    /**
     * Helper method to request permissions. Asks for READ_MEDIA_AUDIO and RECORD_AUDIO.
     * READ_MEDIA_AUDIO is needed for audio playing.
     * RECORD_AUDIO is needed for the visualizer.
     */
    private fun requestPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    /**
     * Helper method to open the file list.
     */
    private fun openFileList() {
        var intent: Intent = Intent(this@MainActivity, FileListActivity::class.java)
        var path: String = Environment.getExternalStorageDirectory().path
        intent.putExtra("path", path)
        startActivity(intent)
    }

    /**
     * Changes the colors based on user's settings. Applies default colors if not.
     */
    private fun changeColors() {
        val color1 = SettingsChange.getColor1(this)
        val color2 = SettingsChange.getColor2(this)
        val color3 = SettingsChange.getColor3(this)

        ColorUtil.updateAllTextColors(findViewById<ScrollView>(R.id.main), color1)

        val exclude = arrayOf<View>(findViewById(R.id.dividerBar), findViewById(R.id.playlistBtn),
            findViewById(R.id.filesBtn), findViewById(R.id.wnBtn),
            findViewById(R.id.settingsBtn))
        ColorUtil.updateAccentColors(
            findViewById<ScrollView>(R.id.main), color3, exclude)

        arrayOf<ImageView>(findViewById(R.id.iconPlaylist), findViewById(R.id.iconFiles),
                findViewById(R.id.iconWn), findViewById(R.id.iconSettings)).forEach {
            it.setBackgroundColor(color2)
            it.setColorFilter(color3) }

        arrayOf<TextView>(findViewById(R.id.plText), findViewById(R.id.fText),
                findViewById(R.id.wnText), findViewById(R.id.sText)).forEach {
            it.setBackgroundColor(color2)
        }

        findViewById<ImageButton>(R.id.titleButton).setColorFilter(color1)
        findViewById<ImageButton>(R.id.titleButton).background = color2.toDrawable()
    }
}
