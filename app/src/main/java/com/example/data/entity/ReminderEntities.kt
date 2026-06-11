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
    val activityType: String, // "WATER", "WALK", "STRETCH", "SNOOZE", "WORKOUT_YOGA"
    val notes: String = ""
)

@Entity(tableName = "linked_devices")
data class LinkedDevice(
    @PrimaryKey val id: String,
    val deviceName: String,
    val deviceType: String, // "WATCH", "RING", "HEALTH_APP"
    val isConnected: Boolean = true,
    val pairedDate: Long = System.currentTimeMillis(),
    val lastSyncTime: Long = System.currentTimeMillis(),
    val batteryPercent: Int = 85
)

@Entity(tableName = "device_logs")
data class DeviceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dataType: String, // "STEPS", "WATER", "WORKOUT", "YOGA", "BLOOD_PRESSURE"
    val value: String,
    val status: String = "SUCCESS", // "SUCCESS", "REJECTED_BY_AI"
    val dateStr: String
)

