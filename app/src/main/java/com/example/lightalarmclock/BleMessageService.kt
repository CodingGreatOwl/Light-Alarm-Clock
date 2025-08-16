package com.example.lightalarmclock

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.*

class BleMessageService : Service() {

    private val bluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }
    private var bluetoothGatt: BluetoothGatt? = null

    // Replace these with your actual BLE device values
    private val deviceAddress = "XX:XX:XX:XX:XX:XX" // Your BLE device MAC address
    private val serviceUuid = UUID.fromString("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    private val characteristicUuid = UUID.fromString("YYYYYYYY-YYYY-YYYY-YYYY-YYYYYYYYYYYY")
    private val predefinedMessage = "YOUR_PREDEFINED_MESSAGE"

    companion object {
        private const val CHANNEL_ID = "BleMessageChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start BLE connection and message sending
        connectAndSendMessage()

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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Message")
            .setContentText("Sending pre-alarm message to BLE device...")
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectAndSendMessage() {
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
        } catch (e: Exception) {
            // Handle connection error
            stopSelf()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // Connection successful, discover services
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // Connection lost or failed
                    cleanup()
                    stopSelf()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(serviceUuid)
                val characteristic = service?.getCharacteristic(characteristicUuid)

                if (characteristic != null) {
                    // Use version-compatible write method
                    writeCharacteristicCompat(gatt, characteristic, predefinedMessage.toByteArray())
                } else {
                    // Characteristic not found
                    cleanup()
                    stopSelf()
                }
            } else {
                // Service discovery failed
                cleanup()
                stopSelf()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Message sent successfully
                updateNotification("Message sent successfully!")
            } else {
                // Write failed
                updateNotification("Failed to send message")
            }

            // Clean up and stop service after a short delay
            android.os.Handler(mainLooper).postDelayed({
                cleanup()
                stopSelf()
            }, 2000)
        }
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Message")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cleanup() {
        bluetoothGatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun writeCharacteristicCompat(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    data: ByteArray
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // API 33+: Use new writeCharacteristic method with direct value parameter
        val result = gatt.writeCharacteristic(
            characteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        result == BluetoothStatusCodes.SUCCESS
    } else {
        // API < 33: Use deprecated setValue + writeCharacteristic
        @Suppress("DEPRECATION")
        characteristic.setValue(data)
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }
}
