package com.example.data.repository

import com.example.data.dao.ReminderDao
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {

    val reminderSettingFlow: Flow<ReminderSetting?> = reminderDao.getReminderSettingFlow()
    val allLogsFlow: Flow<List<ActivityLog>> = reminderDao.getAllLogs()

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

    suspend fun logActivity(type: String, notes: String) {
        reminderDao.insertLog(ActivityLog(activityType = type, notes = notes))
    }

    suspend fun clearLogs() {
        reminderDao.clearLogs()
    }
}
