package com.example.lightalarmclock

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
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
import androidx.core.net.toUri

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var dismissReceiver: BroadcastReceiver
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
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Keep screen on while alarm is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
        setContent {
            LightAlarmClockTheme {
                AlarmScreen(
                    label = "",
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
        registerReceiver(dismissReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
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
                    setDataSource(this@AlarmActivity, soundUri.toUri())
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
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
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
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
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun startVibration() {
        try {

            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator =vibratorManager.defaultVibrator

            // Create vibration pattern: vibrate 1s, pause 1s, repeat
            val pattern = longArrayOf(0, 1000, 1000)

            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
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
        val systemNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        systemNotificationManager.cancel(alarmId)
        stopService(Intent(this, AlarmRingingService::class.java))

        // Close the activity or move on as needed
        finish()
    }

    private fun snoozeAlarm() {
        // Stop current alarm effects
        stopAlarmEffects()

        // Cancel current notification
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(alarmId)

        // Schedule snooze alarm (5 minutes later)
        scheduleSnoozeAlarm()
        stopService(Intent(this, AlarmRingingService::class.java))

        // Close the activity or move on as needed
        finish()
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

        // Schedule using AlarmManager directly for snooze
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", snoozeAlarm.id)
            putExtra("ALARM_LABEL", snoozeAlarm.label)
            putExtra("SOUND_URI", snoozeAlarm.soundUri)
            putExtra("HAS_VIBRATION", snoozeAlarm.hasVibration)
            putExtra("HAS_NOTIFICATION", snoozeAlarm.hasNotification)
        }
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
        unregisterReceiver(dismissReceiver)

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
                modifier = Modifier.size(105.dp),
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
                modifier = Modifier.size(105.dp),
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
