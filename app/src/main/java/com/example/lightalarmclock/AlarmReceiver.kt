package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // This is where you would launch the alarm UI or play a sound.
        Toast.makeText(context, "Alarm ringing!", Toast.LENGTH_LONG).show()
        // You could start an Activity or Service here if you want more complex behavior.
    }
}
