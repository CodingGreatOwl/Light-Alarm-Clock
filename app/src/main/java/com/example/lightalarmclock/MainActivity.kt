package com.example.lightalarmclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.lightalarmclock.ui.theme.LightAlarmClockTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// MainActivity.kt
class MainActivity : ComponentActivity() {
    private lateinit var alarmRepository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmRepository = AlarmRepository(this)

        setContent {
            LightAlarmClockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmClockApp(alarmRepository)
                }
            }
            val bleSettingsViewModel = remember { BleSettingsViewModel() }
        }
    }
}

@Composable
fun AlarmClockApp(alarmRepository: AlarmRepository) {
    val bleSettingsViewModel = remember { BleSettingsViewModel() }
    val alarmsState = remember { mutableStateListOf<Alarm>().apply { addAll(alarmRepository.getAllAlarms()) } }
    var currentScreen by remember { mutableStateOf("list") }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    val context = LocalContext.current

    // Helper to refresh alarms
    fun refreshAlarms() {
        alarmsState.clear()
        alarmsState.addAll(alarmRepository.getAllAlarms())
    }

    when (currentScreen) {
        "list" -> AlarmListScreen(
            alarms = alarmsState,
            onAddAlarm = { currentScreen = "add" },
            onEditAlarm = { alarm ->
                editingAlarm = alarm
                currentScreen = "edit"
            },
            onToggleAlarm = { alarm ->
                alarmRepository.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
                refreshAlarms()
            },
            onDeleteAlarm = { alarm ->
                alarmRepository.deleteAlarm(alarm)
                refreshAlarms()
            },
            onOpenBleSettings = { currentScreen = "bleSettings" }
        )

        "add" -> AddEditAlarmScreen(
            alarm = null,
            onSave = { alarm ->
                alarmRepository.addAlarm(alarm)
                refreshAlarms()
                currentScreen = "list"
            },
            onCancel = { currentScreen = "list" }
        )

        "edit" -> AddEditAlarmScreen(
            alarm = editingAlarm,
            onSave = { alarm ->
                alarmRepository.updateAlarm(alarm)
                refreshAlarms()
                currentScreen = "list"
            },
            onCancel = { currentScreen = "list" }
        )
        "bleSettings" -> BluetoothSettingsScreen(
            deviceAddress = bleSettingsViewModel.deviceAddress.value,
            onDeviceAddressChange = { bleSettingsViewModel.deviceAddress.value = it },
            serviceUuid = bleSettingsViewModel.serviceUuid.value,
            onServiceUuidChange = { bleSettingsViewModel.serviceUuid.value = it },
            message = bleSettingsViewModel.message.value,
            onMessageChange = { bleSettingsViewModel.message.value = it },
            onOpenBluetoothSettings = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            },
            onTestConnection = @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) { bleSettingsViewModel.testConnection(context) },
            connectionStatus = bleSettingsViewModel.connectionStatus.value,
            onSendMessage = @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) { bleSettingsViewModel.sendMessage(context) },
            messageStatus = bleSettingsViewModel.messageStatus.value,
            onBack = { currentScreen = "list" }
        )
    }
}

@Composable
fun BluetoothSettingsScreen(
    deviceAddress: String,
    onDeviceAddressChange: (String) -> Unit,
    serviceUuid: String,
    onServiceUuidChange: (String) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onTestConnection: () -> Unit,
    connectionStatus: String,
    onSendMessage: () -> Unit,
    messageStatus: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Bluetooth Settings", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        OutlinedTextField(
            value = deviceAddress,
            onValueChange = onDeviceAddressChange,
            label = { Text("Device Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = serviceUuid,
            onValueChange = onServiceUuidChange,
            label = { Text("Service UUID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onOpenBluetoothSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open Bluetooth Settings")
        }

        Button(onClick = onTestConnection, modifier = Modifier.fillMaxWidth()) {
            Text("Test Connection")
        }
        if (connectionStatus.isNotEmpty()) {
            Text(
                text = connectionStatus,
                color = if (connectionStatus.contains("Success", ignoreCase = true)) Color.Green else Color.Red,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(onClick = onSendMessage, modifier = Modifier.fillMaxWidth()) {
            Text("Send Message")
        }
        if (messageStatus.isNotEmpty()) {
            Text(
                text = messageStatus,
                color = if (messageStatus.contains("Sent", ignoreCase = true)) Color.Green else Color.Red,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onOpenBleSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alarms",
                style = MaterialTheme.typography.headlineMedium
            )

            Row {
                IconButton(onClick = onOpenBleSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Bluetooth Settings")
                }
                IconButton(onClick = onAddAlarm) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No alarms set")
            }
        } else {
            LazyColumn {
                items(alarms) { alarm ->
                    AlarmListItem(
                        alarm = alarm,
                        onToggle = { onToggleAlarm(alarm) },
                        onEdit = { onEditAlarm(alarm) },
                        onDelete = { onDeleteAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmListItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (alarm.isRecurring) {
                    Text(
                        text = formatRecurringDays(alarm.recurringDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Once",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row {
                    if (alarm.hasVibration) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = "Vibration",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (alarm.hasNotification) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notification",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun AddEditAlarmScreen(
    alarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    var hour by remember { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: 30) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var isRecurring by remember { mutableStateOf(alarm?.isRecurring ?: false) }
    var selectedDays by remember { mutableStateOf(alarm?.recurringDays ?: emptySet()) }
    var hasVibration by remember { mutableStateOf(alarm?.hasVibration ?: true) }
    var hasNotification by remember { mutableStateOf(alarm?.hasNotification ?: true) }
    var soundUri by remember { mutableStateOf(alarm?.soundUri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }

            TextButton(onClick = {
                try {
                    val newAlarm = Alarm(
                        id = alarm?.id ?: System.currentTimeMillis().toInt(),
                        hour = hour,
                        minute = minute,
                        label = label,
                        isRecurring = isRecurring,
                        recurringDays = selectedDays,
                        hasVibration = hasVibration,
                        hasNotification = hasNotification,
                        soundUri = soundUri
                    )
                onSave(newAlarm)
                } catch (e: Exception) {
                    // Log the error - this will help you debug
                    println("Error saving alarm: ${e.message}")
                    e.printStackTrace()
                }
            }) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time picker
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberPickerField(
                label = "Hour",
                value = hour,
                valueRange = 0..23,
                onValueChange = { hour = it }
            )
            NumberPickerField(
                label = "Minute",
                value = minute,
                valueRange = 0..59,
                onValueChange = { minute = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Label
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Alarm Label") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recurring toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Repeat")
            Switch(
                checked = isRecurring,
                onCheckedChange = { isRecurring = it }
            )
        }

        // Days selection (if recurring)
        if (isRecurring) {
            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekSelector(
                selectedDays = selectedDays,
                onDaysChanged = { selectedDays = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vibration")
            Switch(
                checked = hasVibration,
                onCheckedChange = { hasVibration = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notification")
            Switch(
                checked = hasNotification,
                onCheckedChange = { hasNotification = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound selection
        Button(
            onClick = { /* TODO: Implement sound picker */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (soundUri != null) "Change Sound" else "Select Sound")
        }
    }
}

// Custom NumberPickerField implementation
@Composable
fun NumberPickerField(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("%02d".format(value))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                valueRange.forEach { v ->
                    DropdownMenuItem(text = { Text("%02d".format(v)) }, onClick = {
                        onValueChange(v)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = selectedDays.contains(day.value)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        selectedDays - day.value
                    } else {
                        selectedDays + day.value
                    }
                    onDaysChanged(newDays)
                },
                label = { Text(day.shortName) }
            )
        }
    }
}

fun formatRecurringDays(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays"
    if (days == setOf(6, 7)) return "Weekends"

    return days.sorted().joinToString(", ") { dayValue ->
        DayOfWeek.entries.find { it.value == dayValue }?.shortName ?: ""
    }
}

