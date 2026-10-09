import android.content.Context

class BlePreferencesManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("ble_settings", Context.MODE_PRIVATE)

    fun saveSettings(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        message: String
    ) {
        val editor = prefs.edit()
        editor.putString("device_address", deviceAddress)
        editor.putString("service_uuid", serviceUuid)
        editor.putString("characteristic_uuid", characteristicUuid)
        editor.putString("message", message)
        editor.commit()
    }

    fun getDeviceAddress(): String =
        prefs.getString("device_address", "10:52:1C:66:35:CE") ?: "10:52:1C:66:35:CE"

    fun getServiceUuid(): String =
        prefs.getString("service_uuid", "4fafc201-1fb5-459e-8fcc-c5c9c331914b") ?: ""

    fun getCharacteristicUuid(): String =
        prefs.getString("characteristic_uuid", "beb5483e-36e1-4688-b7f5-ea07361b26a8") ?: ""

    fun getMessage(): String =
        prefs.getString("message", "Hello Bananas") ?: "Hello Bananas"
}

