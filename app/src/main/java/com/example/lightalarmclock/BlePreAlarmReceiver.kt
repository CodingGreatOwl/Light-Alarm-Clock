package com.example.lightalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BlePreAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Start the BLE message sending service or logic
        val serviceIntent = Intent(context, BleMessageService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
