package com.codelab.c14240069.foregroundservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private lateinit var statusTextView: TextView
    private lateinit var btnStart: Button

    private lateinit var btnLatihan: Button

    companion object {
        const val ACTON_SERVICE_STOPPED_JUMP = "com.codelab.c14240069.foregroundservice.ACTION_SERVICE_STOPPED_JUMP"
        const val ACTION_EXERCISE_FINISHED = "com.codelab.c14240069.foregroundservice.ACTION_EXERCISE_FINISHED"
        const val EXTRA_IS_EXERCISE = "EXTRA_IS_EXERCISE"
        const val EXTRA_JUMP_COUNT = "EXTRA_JUMP_COUNT"
        const val REQUEST_CODE_PERMISSIONS = 101
    }

    private fun hasAllPermissions(): Boolean {
        return permissionToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Foreground Service sedang berjalan..."
    }

    private fun startExerciseService() {
        val serviceIntent = Intent(this, TrackerService::class.java).apply {
            putExtra(EXTRA_IS_EXERCISE, true)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Mode Latihan sedang berjalan..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                val serviceIntent = Intent(this, TrackerService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
                finish()
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan"
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent?.action == ACTON_SERVICE_STOPPED_JUMP) {
            statusTextView.text = "Foreground service sudah berhenti karena Anda melompat"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusTextView = findViewById<TextView>(R.id.statusTextView)
        btnStart = findViewById<Button>(R.id.btnStart)
        btnLatihan = findViewById<Button>(R.id.btnLatihan)

        btnLatihan.setOnClickListener {
            if (hasAllPermissions()) {
                startExcerciseService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnStart.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        handleIntent(intent)
    }
}