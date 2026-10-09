package com.example.lightalarmclock

import android.provider.Settings
import android.os.Build
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

class AlarmHelper(private val context: Context, private val alarmRepository: AlarmRepository) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return
            }
        }
        if (!alarm.isEnabled) return

        if (alarm.isRecurring) {
            scheduleRecurringAlarm(alarm)
        } else {
            scheduleOneTimeAlarm(alarm)
        }
    }

    private fun requestExactAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
    }

    private fun scheduleOneTimeAlarm(alarm: Alarm) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleAlarmAtTime(alarm, calendar.timeInMillis)
    }

    private fun scheduleRecurringAlarm(alarm: Alarm) {
        alarm.recurringDays.forEach { dayOfWeek ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, dayOfWeek)

                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            scheduleAlarmAtTime(alarm, calendar.timeInMillis, dayOfWeek)
        }
    }

    private fun scheduleAlarmAtTime(alarm: Alarm, timeMillis: Long, dayOfWeek: Int? = null) {
        val requestId = if (dayOfWeek != null) alarm.id * 10 + dayOfWeek else alarm.id

        // Main alarm
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("SOUND_URI", alarm.soundUri)
            putExtra("HAS_VIBRATION", alarm.hasVibration)
            putExtra("HAS_NOTIFICATION", alarm.hasNotification)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeMillis,
            pendingIntent
        )

        // BLE pre-alarm (10 minutes before)
        val bleIntent = Intent(context, BlePreAlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }

        val blePendingIntent = PendingIntent.getBroadcast(
            context,
            requestId + 1000,
            bleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeMillis - 10 * 60 * 1000,
            blePendingIntent
        )
    }

    fun scheduleAlarmWithDelay(alarmId: Int, delayMillis: Long) {
        val alarm = alarmRepository.getAlarmById(alarmId) ?: return
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MILLISECOND, delayMillis.toInt())
        }
        scheduleAlarmAtTime(alarm, calendar.timeInMillis)
    }

    fun cancelAlarm(alarm: Alarm) {
        if (alarm.isRecurring) {
            alarm.recurringDays.forEach { dayOfWeek ->
                cancelAlarmWithId(alarm.id * 10 + dayOfWeek)
                cancelAlarmWithId(alarm.id * 10 + dayOfWeek + 1000)
            }
        } else {
            cancelAlarmWithId(alarm.id)
            cancelAlarmWithId(alarm.id + 1000)
        }
    }

    private fun cancelAlarmWithId(requestId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
