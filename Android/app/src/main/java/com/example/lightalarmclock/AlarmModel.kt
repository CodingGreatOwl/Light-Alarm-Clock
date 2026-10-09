package com.example.lightalarmclock

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val recurringDays: Set<Int> = emptySet(), // 1=Sunday, 2=Monday, etc.
    val label: String = "",
    val soundUri: String? = null,
    val hasVibration: Boolean = true,
    val hasNotification: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// Days of week helper
enum class DayOfWeek(val value: Int, val shortName: String) {
    MONDAY(1, "Mo"),
    TUESDAY(2, "Tu"),
    WEDNESDAY(3, "We"),
    THURSDAY(4, "Th"),
    FRIDAY(5, "Fr"),
    SATURDAY(6, "Sa"),
    SUNDAY(7, "Su")
}
