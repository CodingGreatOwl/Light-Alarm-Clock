package com.example.lightalarmclock

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

// AlarmRepository.kt
class AlarmRepository(private val context: Context) {

    fun getAlarmById(alarmId: Int): Alarm? {
        return getAllAlarms().firstOrNull { it.id == alarmId }
    }
    private val sharedPrefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmHelper = AlarmHelper(context, this)

    fun getAllAlarms(): List<Alarm> {
        val alarmsJson = sharedPrefs.getString("alarms_list", "[]")
        val type = object : TypeToken<List<Alarm>>() {}.type
        return gson.fromJson(alarmsJson, type) ?: emptyList()
    }

    fun addAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        alarms.add(alarm)
        saveAlarms(alarms)

        if (alarm.isEnabled) {
            alarmHelper.scheduleAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            // Cancel old alarm
            alarmHelper.cancelAlarm(alarms[index])

            // Update and reschedule if enabled
            alarms[index] = alarm
            saveAlarms(alarms)

            if (alarm.isEnabled) {
                alarmHelper.scheduleAlarm(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        val alarms = getAllAlarms().toMutableList()
        alarms.removeAll { it.id == alarm.id }
        saveAlarms(alarms)

        alarmHelper.cancelAlarm(alarm)
    }

    private fun saveAlarms(alarms: List<Alarm>) {
        val alarmsJson = gson.toJson(alarms)
        sharedPrefs.edit { putString("alarms_list", alarmsJson) }
    }
}
