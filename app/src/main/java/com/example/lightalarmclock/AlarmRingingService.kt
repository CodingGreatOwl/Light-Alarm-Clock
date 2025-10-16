package com.example.lightalarmclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val channelId = "alarm_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Display a foreground notification so Android keeps the service alive.
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm is ringing")
            .setContentText("Tap to open alarm screen")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        // Start the alarm sound
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The alarm sound runs continuously until user stops it.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
