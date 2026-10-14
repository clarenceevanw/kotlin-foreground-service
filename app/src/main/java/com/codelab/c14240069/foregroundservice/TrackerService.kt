package com.codelab.c14240069.foregroundservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "tracker_service_running"
    private val NOTIFICATION_CHANNEL_ID_JUMP_DETECTED = "tracker_jump_detected"
    private val NOTIFICATION_ID_SERVICE_RUNNING = 1
    private val NOTIFICATION_ID_JUMP_DETECTED = 2

    private var isExerciseMode = false
    private var jumpCount = 0
    private var stepCount = 0
    private var lastEventTime = 0L

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING,
                "Service Running Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val jumpChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_JUMP_DETECTED,
                "Jump Detected Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(jumpChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
        Log.d("TrackerService", "Service created")
    }

    private fun startJumpDetection() {
        accelerometer?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun finishExercise() {
        sensorManager.unregisterListener(this)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TrackerService", "Service onStartCommand")

        isExerciseMode = intent?.getBooleanExtra(MainActivity.EXTRA_IS_EXERCISE, false) ?: false
        if (isExerciseMode) {
            jumpCount = 0
            stepCount = 0
            Log.d("TrackerService", "Mode Latihan Dimulai!")
        }

        startForeground(
            NOTIFICATION_ID_SERVICE_RUNNING,
            buildServiceRunningNotification()
        )

        startJumpDetection()
        return START_STICKY
    }

    private fun buildServiceRunningNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
            .setContentTitle("Pelacak Aktivitas Latar")
            .setContentText("Aplikasi Tracker Services sedang berjalan...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]
            val jumpThreshold = 15.0
            val stepThreshold = 12.0

            if (!isExerciseMode) {
                if (abs(y) > jumpThreshold) {
                    Log.d("TrackerService", "LOMPATAN terdeteksi! y:$y")

                    sensorManager.unregisterListener(this)

                    showJumpNotification()
                    stopSelf()
                }
            } else {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEventTime > 500) {
                    if (abs(y) > jumpThreshold) {
                        jumpCount++
                        lastEventTime = currentTime
                        Log.d("TrackerService", "Latihan - Lompatan ke-$jumpCount (y: $y)")
                    } else if (abs(y) > stepThreshold) {
                        stepCount++
                        lastEventTime = currentTime
                        Log.d("TrackerService", "Latihan - Langkah ke-$stepCount (y: $y)")

                        if (stepCount > 2) {
                            Log.d("TrackerService", "Latihan Selesai! Total Lompatan: $jumpCount, Langkah: $stepCount")
                            finishExercise()
                        }
                    }
                }
            }
        }
    }

    private fun showJumpNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTON_SERVICE_STOPPED_JUMP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingMainActivityIntent = PendingIntent.getActivity(
            this, 1, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val jumpNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("Lompatan Terdeteksi")
            .setContentText("Service berhenti. Ketuk untuk membuka aktivitas")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED, jumpNotification)
        } catch (e: SecurityException) {
            Log.e("TrackerService", "Gagal memunculkan notifikasi karena permission POST_NOTIFICATION tidak aktif: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("TrackerService", "Service onDestroy")
    }
}