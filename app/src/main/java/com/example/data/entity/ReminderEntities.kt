package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSetting(
    @PrimaryKey val id: Int = 1, // Single global configuration profile for ultra-UX simplicity
    val frequencyMinutes: Int = 45,
    val drinkWaterEnabled: Boolean = true,
    val walkEnabled: Boolean = true,
    val stretchEnabled: Boolean = true,
    val mindfulEnabled: Boolean = true,
    val alertSound: String = "Muted Chimes", // "Muted Chimes", "Zen Wood Block", "Cosmic Resonance", "Silent"
    val vibrationPattern: String = "Soft Pulse", // "Soft Pulse", "Steady Sync", "Continuous Wave", "No Vibration"
    val repetitionType: String = "WEEKDAYS", // "EVERYDAY", "WEEKDAYS", "WEEKENDS"
    val maxOccurrencesPerDay: Int = 8, // Repeats repeat-count limit
    val completedOccurrencesToday: Int = 0,
    val nextScheduledTime: Long = System.currentTimeMillis() + (45 * 60 * 1000),
    val lastTriggeredTime: Long = 0,
    val snoozedUntil: Long = 0,
    val isActive: Boolean = true,
    val themeName: String = "LAVENDER"
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val activityType: String, // "WATER", "WALK", "STRETCH", "SNOOZE"
    val notes: String = ""
)
