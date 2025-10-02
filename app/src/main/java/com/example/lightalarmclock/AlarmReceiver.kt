package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getIntExtra("ALARM_ID", 0) ?: 0
        val label = intent?.getStringExtra("ALARM_LABEL")
        val soundUri = intent?.getStringExtra("SOUND_URI")
        val hasVibration = intent?.getBooleanExtra("HAS_VIBRATION", true) ?: true
        val hasNotification = intent?.getBooleanExtra("HAS_NOTIFICATION", true) ?: true

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("SOUND_URI", soundUri)
            putExtra("HAS_VIBRATION", hasVibration)
            putExtra("HAS_NOTIFICATION", hasNotification)
        }
        context.startActivity(alarmIntent)
    }
}
