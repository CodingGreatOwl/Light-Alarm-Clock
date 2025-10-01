package com.example.lightalarmclock

import BlePreferencesManager
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class BleMessageService : Service() {
    private lateinit var prefsManager: BlePreferencesManager

    companion object {
        private const val CHANNEL_ID = "BleMessageChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefsManager = BlePreferencesManager(this)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Connecting to BLE device…")
        startForeground(NOTIFICATION_ID, notification)

        // Read settings from SharedPreferences instead of Intent extras
        val address = prefsManager.getDeviceAddress()
        val serviceUuid = prefsManager.getServiceUuid()
        val characteristicUuid = prefsManager.getCharacteristicUuid()
        val message = prefsManager.getMessage()


        // Use BleManager to handle the actual BLE communication
        BleManager(this).connectAndSend(
            deviceAddress = address,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            message = message
        ) { success, statusMessage ->
            updateNotification(statusMessage)
            Handler(mainLooper).postDelayed({
                stopSelf()
            }, 2000)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun startBleMessageService(context: Context, viewModel: BleSettingsViewModel) {
        val intent = Intent(context, BleMessageService::class.java).apply {
            putExtra("ADDRESS", viewModel.deviceAddress.value)
            putExtra("SERVICE_UUID", viewModel.serviceUuid.value)
            putExtra("CHARACTERISTIC_UUID", viewModel.characteristicUuid.value)
            putExtra("MESSAGE", viewModel.message.value)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE Message Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sending message to BLE device"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Message")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification) // Make sure you have this icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // No GATT cleanup here; BleManager is handling it per operation
    }
}
