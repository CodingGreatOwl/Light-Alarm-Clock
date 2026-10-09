package com.example.lightalarmclock

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class BleMessageService : Service() {
    private lateinit var dataStore: BleSettingsDataStore
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "BleMessageChannel"
        private const val NOTIFICATION_ID = 1001

        fun startBleMessageService(context: Context) {
            val intent = Intent(context, BleMessageService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        dataStore = BleSettingsDataStore(this)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Connecting to BLE device…")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                // Read settings from DataStore
                val settings = dataStore.getSettings()

                withContext(Dispatchers.Main) {
                    // Use BleManager to handle the actual BLE communication
                    BleManager(this@BleMessageService).connectAndSend(
                        deviceAddress = settings.deviceAddress,
                        serviceUuid = settings.serviceUuid,
                        characteristicUuid = settings.characteristicUuid,
                        message = settings.message
                    ) { success, statusMessage ->
                        updateNotification(statusMessage)
                        Handler(mainLooper).postDelayed({
                            stopSelf()
                        }, 2000)
                    }
                }
            } catch (e: Exception) {
                updateNotification("Failed to read BLE settings: ${e.message}")
                Handler(mainLooper).postDelayed({
                    stopSelf()
                }, 2000)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon as fallback
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
        serviceScope.cancel()
    }
}