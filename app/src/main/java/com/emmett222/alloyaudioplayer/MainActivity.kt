package com.emmett222.alloyaudioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.arrayOf

/**
 * Opening screen for Alloy. Asks for permissions if needed and takes user to file screen.
 *
 * @author Emmett Grebe
 * @version 5-27-2026
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

        filesBtn.setOnClickListener {
            if (checkPermission()) {
                openFileList()
            } else {
                requestPermission()
            }
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
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
}
