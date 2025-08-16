package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getIntExtra("ALARM_ID", 0) ?: 0

        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(alarmId)

        // Optional: Stop any ongoing alarm sound/vibration
        // This would require a service or broadcast to the AlarmActivity
        val dismissIntent = Intent(context, AlarmActivity::class.java).apply {
            action = "DISMISS_ALARM"
            putExtra("ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Send broadcast to dismiss active alarm if AlarmActivity is running
        val broadcastIntent = Intent("com.example.lightalarmclock.DISMISS_ALARM").apply {
            putExtra("ALARM_ID", alarmId)
        }
        context.sendBroadcast(broadcastIntent)
    }
}
