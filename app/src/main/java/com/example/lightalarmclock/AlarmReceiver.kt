package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // NEW: Launching the Foreground Service that handles sound and notifications.
        val serviceIntent = Intent(context, AlarmRingingService::class.java)

        // Starting foreground service based on Android version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)  // Required for Android O and above
        } else {
            context.startService(serviceIntent)
        }
    }
}
