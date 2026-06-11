package com.example.data.repository

import com.example.data.dao.ReminderDao
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import com.example.data.entity.LinkedDevice
import com.example.data.entity.DeviceLog
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {

    val reminderSettingFlow: Flow<ReminderSetting?> = reminderDao.getReminderSettingFlow()
    val allLogsFlow: Flow<List<ActivityLog>> = reminderDao.getAllLogs()
    val linkedDevicesFlow: Flow<List<LinkedDevice>> = reminderDao.getAllDevicesFlow()
    val deviceLogsFlow: Flow<List<DeviceLog>> = reminderDao.getAllDeviceLogsFlow()

    suspend fun getReminderSetting(): ReminderSetting {
        var setting = reminderDao.getReminderSetting()
        if (setting == null) {
            setting = ReminderSetting()
            reminderDao.insertReminderSetting(setting)
        }
        return setting
    }

    suspend fun saveReminderSetting(setting: ReminderSetting) {
        reminderDao.insertReminderSetting(setting)
    }

    suspend fun saveLog(log: ActivityLog) {
        reminderDao.insertLog(log)
    }

    suspend fun logActivity(type: String, notes: String) {
        reminderDao.insertLog(ActivityLog(activityType = type, notes = notes))
    }

    suspend fun clearLogs() {
        reminderDao.clearLogs()
    }

    // --- Linked Devices CRUD ---
    suspend fun saveDevice(device: LinkedDevice) {
        reminderDao.insertDevice(device)
    }

    suspend fun removeDevice(id: String) {
        reminderDao.deleteDevice(id)
    }

    suspend fun getDeviceById(id: String): LinkedDevice? {
        return reminderDao.getDeviceById(id)
    }

    // --- Device Sync Logs CRUD ---
    suspend fun saveDeviceLog(log: DeviceLog) {
        reminderDao.insertDeviceLog(log)
    }

    suspend fun clearDeviceLogs() {
        reminderDao.clearDeviceLogs()
    }
}

