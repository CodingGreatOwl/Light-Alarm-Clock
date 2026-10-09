package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "ACTION_SNOOZE" -> {
                // Handle Snooze Action
                val alarmId = intent.getIntExtra("ALARMID", 0)
                val alarmHelper = AlarmHelper(context, AlarmRepository(context))
                // Schedule alarm snooze (10 minutes delay)
                alarmHelper.scheduleAlarmWithDelay(alarmId, 10 * 60 * 1000L)

                // Dismiss current notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(alarmId)

                return
            }
        }
        val alarmId = intent?.getIntExtra("ALARMID", 0) ?: 0

        // Cancel the notification associated with the alarm
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(alarmId)

        // Stop the AlarmRingingService to stop the alarm sound
        val stopIntent = Intent(context, AlarmRingingService::class.java)
        context.stopService(stopIntent)

        // Optionally send broadcast to AlarmActivity for UI update
        val broadcastIntent = Intent("com.example.lightalarmclock.DISMISSALARM").apply {
            putExtra("ALARMID", alarmId)
        }
        context.sendBroadcast(broadcastIntent)
    }
}
