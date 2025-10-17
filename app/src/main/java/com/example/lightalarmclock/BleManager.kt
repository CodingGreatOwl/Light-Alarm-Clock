package com.example.lightalarmclock

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import java.util.*

class BleManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectAndSend(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        message: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = adapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    onResult(true, "Connection successful")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onResult(true, "Service found")
                    val service = gatt.getService(UUID.fromString(serviceUuid))
                    val charac = service?.getCharacteristic(UUID.fromString(characteristicUuid))
                    if (charac != null) {
                        writeCharacteristicCompat(gatt, charac, message.toByteArray(), onResult)
                        onResult(true, "Message Sent")
                    } else {
                        onResult(false, "Characteristic not found")
                    }
                } else {
                    onResult(false, "Service discovery failed")
                }
                gatt.disconnect()
                gatt.close()
            }

            /*@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onCharacteristicWrite(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.disconnect()
                    gatt.close()
                }
            }*/
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        onResult: (Boolean, String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            onResult(result == BluetoothStatusCodes.SUCCESS, if (result == BluetoothStatusCodes.SUCCESS) "Message sent" else "Write failed")
            gatt.disconnect()
            gatt.close()
        } else {
            @Suppress("DEPRECATION")
            characteristic.setValue(data)
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
            gatt.disconnect()
            gatt.close()
        }
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun testConnection(
        deviceAddress: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = adapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onResult(true, "Connection successful")
                    gatt.disconnect()
                    gatt.close()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onResult(false, "Disconnected")
                    gatt.close()
                }
            }
        })
    }
}
