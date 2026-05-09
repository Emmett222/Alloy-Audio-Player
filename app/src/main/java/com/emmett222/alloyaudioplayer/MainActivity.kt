package com.emmett222.alloyaudioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var storageBtn: Button = findViewById(R.id.storage_btn)

        storageBtn.setOnClickListener {
            if (checkPermission()) {
                var intent: Intent = Intent(this@MainActivity, FileListActivity::class.java)
                var path: String = Environment.getExternalStorageDirectory().path
                intent.putExtra("path", path)
                startActivity(intent)
            } else {
                requestPermission()
            }


        }
    }

    private fun checkPermission(): Boolean {
        var result = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        ) {
            Toast.makeText(this@MainActivity, "Needs storage permission!", Toast.LENGTH_SHORT)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                111
            )
        }


    }
}
