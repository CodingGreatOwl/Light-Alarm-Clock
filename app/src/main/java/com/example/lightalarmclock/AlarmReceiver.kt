package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Toast.makeText(context, "Alarm triggered! Starting service...", Toast.LENGTH_LONG).show()

        // Retrieve ringtone URI string from intent extras (assuming your alarm data includes this)
        val ringtoneUri = intent?.getStringExtra("SOUND_URI") ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

        // Launch AlarmActivity to bring dismiss UI to foreground
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("SOUND_URI", ringtoneUri)
        }
        context.startActivity(activityIntent)

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra("SOUND_URI", ringtoneUri)
        }

        context.startForegroundService(serviceIntent)
    }
}
