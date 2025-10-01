package com.example.lightalarmclock

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ble_settings")

class BleSettingsDataStore(private val context: Context) {

    companion object {
        private val DEVICE_ADDRESS = stringPreferencesKey("device_address")
        private val SERVICE_UUID = stringPreferencesKey("service_uuid")
        private val CHARACTERISTIC_UUID = stringPreferencesKey("characteristic_uuid")
        private val MESSAGE = stringPreferencesKey("message")
    }

    suspend fun saveSettings(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        message: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ADDRESS] = deviceAddress
            prefs[SERVICE_UUID] = serviceUuid
            prefs[CHARACTERISTIC_UUID] = characteristicUuid
            prefs[MESSAGE] = message
        }
    }

    val bleSettingsFlow: Flow<BleSettings> = context.dataStore.data
        .map { prefs ->
            BleSettings(
                deviceAddress = prefs[DEVICE_ADDRESS] ?: "10:52:1C:66:35:CE",
                serviceUuid = prefs[SERVICE_UUID] ?: "4fafc201-1fb5-459e-8fcc-c5c9c331914b",
                characteristicUuid = prefs[CHARACTERISTIC_UUID] ?: "beb5483e-36e1-4688-b7f5-ea07361b26a8",
                message = prefs[MESSAGE] ?: "Hello Bananas"
            )
        }

    suspend fun getSettings(): BleSettings {
        return bleSettingsFlow.first()
    }
}

data class BleSettings(
    val deviceAddress: String,
    val serviceUuid: String,
    val characteristicUuid: String,
    val message: String
)
