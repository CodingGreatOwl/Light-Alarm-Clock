package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Toast.makeText(context, "Alarm triggered! Starting service...", Toast.LENGTH_LONG).show()

        // Retrieve ringtone URI string from intent extras (assuming your alarm data includes this)
        val ringtoneUri = intent?.getStringExtra("RINGTONE_URI") ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
