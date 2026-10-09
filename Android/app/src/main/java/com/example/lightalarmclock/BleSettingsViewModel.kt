package com.example.lightalarmclock

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BleSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = BleSettingsDataStore(application)

    var deviceAddress = mutableStateOf("")
    var serviceUuid = mutableStateOf("")
    var characteristicUuid = mutableStateOf("")
    var message = mutableStateOf("")
    var connectionStatus = mutableStateOf("")
    var messageStatus = mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStore.bleSettingsFlow.collect { settings ->
                deviceAddress.value = settings.deviceAddress
                serviceUuid.value = settings.serviceUuid
                characteristicUuid.value = settings.characteristicUuid
                message.value = settings.message
            }
        }
    }

    fun updateSettings() {
        viewModelScope.launch {
            dataStore.saveSettings(
                deviceAddress.value,
                serviceUuid.value,
                characteristicUuid.value,
                message.value
            )
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun testConnection(context: Context) {
        BleManager(context).testConnection(
            deviceAddress.value
        ) { success, message -> connectionStatus.value = message }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendMessage(context: Context) {
        BleManager(context).connectAndSend(
            deviceAddress.value,
            serviceUuid.value,
            characteristicUuid.value,
            message.value
        ) { success, msg -> messageStatus.value = msg }
    }
}
