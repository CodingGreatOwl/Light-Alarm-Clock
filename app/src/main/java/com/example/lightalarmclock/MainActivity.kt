package com.example.lightalarmclock

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lightalarmclock.ui.theme.LightAlarmClockTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var alarmRepository: AlarmRepository

    private var selectedSoundUri by mutableStateOf<String?>(null)

    private val ringtonePickerLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                selectedSoundUri = uri?.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmRepository = AlarmRepository(this)

        setContent {
            LightAlarmClockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmClockApp(
                        alarmRepository = alarmRepository,
                        selectedSoundUri = selectedSoundUri,
                        onSelectSound = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                selectedSoundUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmClockApp(
    alarmRepository: AlarmRepository,
    selectedSoundUri: String?,
    onSelectSound: () -> Unit
) {
    val context = LocalContext.current

    val bleSettingsViewModel: BleSettingsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )

    val alarmsState = remember { mutableStateListOf<Alarm>().apply { addAll(alarmRepository.getAllAlarms()) } }
    var currentScreen by remember { mutableStateOf("list") }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

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
                // Inject the selected sound URI into the alarm before saving
                val alarmWithSound = alarm.copy(soundUri = selectedSoundUri)
                alarmRepository.addAlarm(alarmWithSound)
                refreshAlarms()
                currentScreen = "list"
            },
            onCancel = { currentScreen = "list" },
            soundUri = selectedSoundUri,
            onSelectSound = onSelectSound
        )

        "edit" -> AddEditAlarmScreen(
            alarm = editingAlarm,
            onSave = { alarm ->
                val alarmWithSound = alarm.copy(soundUri = selectedSoundUri)
                alarmRepository.updateAlarm(alarmWithSound)
                refreshAlarms()
                currentScreen = "list"
            },
            onCancel = { currentScreen = "list" },
            soundUri = selectedSoundUri,
            onSelectSound = onSelectSound
        )

        "bleSettings" -> BleSettingsScreen(
            viewModel = bleSettingsViewModel,
            onBack = { currentScreen = "list" }
        )
    }
}

@Composable
fun AddEditAlarmScreen(
    alarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit,
    soundUri: String?,
    onSelectSound: () -> Unit
) {
    var hour by remember { mutableStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableStateOf(alarm?.minute ?: 30) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var isRecurring by remember { mutableStateOf(alarm?.isRecurring ?: false) }
    var selectedDays by remember { mutableStateOf(alarm?.recurringDays ?: emptySet()) }
    var hasVibration by remember { mutableStateOf(alarm?.hasVibration ?: true) }
    var hasNotification by remember { mutableStateOf(alarm?.hasNotification ?: true) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
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
            NumberPickerWheel(
                label = "Hour",
                value = hour,
                valueRange = 0..23,
                onValueChange = { hour = it }
            )
            NumberPickerWheel(
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
            onClick = onSelectSound,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (soundUri != null) "Change Sound" else "Select Sound")
        }
    }
}

@Composable
fun NumberPickerWheel(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeSize = valueRange.last - valueRange.first + 1
    val multiplier = 1000 // Repeat range 1000 times for smooth infinite scroll

    // Large list size of repeated values
    val listSize = rangeSize * multiplier

    // Calculate initial index in the big list for the given value
    val initialIndex = (listSize / 2) - (listSize / 2) % rangeSize + (value - valueRange.first)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))

        Box(
            modifier = Modifier
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 50.dp)
            ) {
                items(listSize) { index ->
                    val itemValue = valueRange.first + index % rangeSize
                    val isSelected = index == listState.firstVisibleItemIndex
                    Text(
                        text = "%02d".format(itemValue),
                        style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .alpha(if (isSelected) 1f else 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(40.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            )
        }

        // Snap and update selection on scroll end
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                val centeredIndex = listState.firstVisibleItemIndex
                val newValue = valueRange.first + centeredIndex % rangeSize
                if (newValue != value) {
                    onValueChange(newValue)
                    coroutineScope.launch {
                        listState.animateScrollToItem(centeredIndex)
                    }
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

fun formatRecurringDays(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays"
    if (days == setOf(6, 7)) return "Weekends"

    return days.sorted().joinToString(", ") { dayValue ->
        DayOfWeek.entries.find { it.value == dayValue }?.shortName ?: ""
    }
}

