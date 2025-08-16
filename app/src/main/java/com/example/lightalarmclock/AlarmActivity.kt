package com.example.lightalarmclock

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.lightalarmclock.ui.theme.LightAlarmClockTheme
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var dismissReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())
    private var alarmId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make this activity work over lock screen and turn on screen
        setupLockScreenFlags()

        // Get alarm data from intent
        alarmId = intent.getIntExtra("ALARM_ID", 0)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val soundUri = intent.getStringExtra("SOUND_URI")
        val hasVibration = intent.getBooleanExtra("HAS_VIBRATION", true)

        // Setup dismiss receiver
        setupDismissReceiver()

        // Start alarm effects
        startAlarmEffects(soundUri, hasVibration)

        // Setup UI
        setContent {
            LightAlarmClockTheme {
                AlarmScreen(
                    label = label,
                    onDismiss = {
                        dismissAlarm()
                        finish()
                    },
                    onSnooze = {
                        snoozeAlarm()
                        finish()
                    }
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on while alarm is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupDismissReceiver() {
        dismissReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.lightalarmclock.DISMISS_ALARM") {
                    val dismissedAlarmId = intent.getIntExtra("ALARM_ID", 0)
                    if (dismissedAlarmId == alarmId) {
                        dismissAlarm()
                        finish()
                    }
                }
            }
        }

        val intentFilter = IntentFilter("com.example.lightalarmclock.DISMISS_ALARM")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, intentFilter)
        }
    }

    private fun startAlarmEffects(soundUri: String?, hasVibration: Boolean) {
        // Start alarm sound
        startAlarmSound(soundUri)

        // Start vibration if enabled
        if (hasVibration) {
            startVibration()
        }
    }

    private fun startAlarmSound(soundUri: String?) {
        try {
            mediaPlayer = if (!soundUri.isNullOrEmpty()) {
                // Use custom sound
                MediaPlayer().apply {
                    setDataSource(this@AlarmActivity, Uri.parse(soundUri))
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                // Use default alarm sound
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                MediaPlayer.create(this, defaultUri)?.apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try to create a basic beep sound
            createFallbackSound()
        }
    }

    private fun createFallbackSound() {
        try {
            // Create a simple notification sound as fallback
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(this, notificationUri)?.apply {
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Create vibration pattern: vibrate 1s, pause 1s, repeat
            val pattern = longArrayOf(0, 1000, 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissAlarm() {
        // Stop all alarm effects
        stopAlarmEffects()

        // Cancel notification
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(alarmId)

        // Clear any pending notifications
        val systemNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemNotificationManager.cancel(alarmId)
    }

    private fun snoozeAlarm() {
        // Stop current alarm effects
        stopAlarmEffects()

        // Cancel current notification
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(alarmId)

        // Schedule snooze alarm (5 minutes later)
        scheduleSnoozeAlarm()
    }

    private fun scheduleSnoozeAlarm() {
        val snoozeTimeMillis = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
        val snoozeAlarm = Alarm(
            id = alarmId + 50000, // Different ID for snooze to avoid conflicts
            hour = 0, // Will be calculated from snoozeTimeMillis
            minute = 0, // Will be calculated from snoozeTimeMillis
            isEnabled = true,
            isRecurring = false,
            label = "Snooze: ${intent.getStringExtra("ALARM_LABEL") ?: "Alarm"}",
            soundUri = intent.getStringExtra("SOUND_URI"),
            hasVibration = intent.getBooleanExtra("HAS_VIBRATION", true),
            hasNotification = intent.getBooleanExtra("HAS_NOTIFICATION", true)
        )

        // Use AlarmHelper to schedule the snooze
        val alarmHelper = AlarmHelper(this)

        // Create a temporary alarm for snooze scheduling
        val calendar = Calendar.getInstance().apply {
            timeInMillis = snoozeTimeMillis
        }

        // Schedule using AlarmManager directly for snooze
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", snoozeAlarm.id)
            putExtra("ALARM_LABEL", snoozeAlarm.label)
            putExtra("SOUND_URI", snoozeAlarm.soundUri)
            putExtra("HAS_VIBRATION", snoozeAlarm.hasVibration)
            putExtra("HAS_NOTIFICATION", snoozeAlarm.hasNotification)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            snoozeAlarm.id,
            snoozeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            snoozeTimeMillis,
            pendingIntent
        )
    }

    private fun stopAlarmEffects() {
        // Stop sound
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null

        // Stop vibration
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        stopAlarmEffects()

        // Unregister receiver
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    override fun onBackPressed() {
        // Prevent back button from dismissing alarm
        // User must explicitly dismiss or snooze
    }
}

@Composable
fun AlarmScreen(
    label: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Current time display
        Text(
            text = currentTime,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Alarm label
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))
        } else {
            Spacer(modifier = Modifier.height(72.dp))
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snooze button
            Button(
                onClick = onSnooze,
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "Snooze",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Additional info
        Text(
            text = "Swipe to snooze for 5 minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun getCurrentTimeString(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
