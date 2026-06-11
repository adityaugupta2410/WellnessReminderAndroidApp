package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_settings WHERE id = :id LIMIT 1")
    fun getReminderSettingFlow(id: Int = 1): Flow<ReminderSetting?>

    @Query("SELECT * FROM reminder_settings WHERE id = :id LIMIT 1")
    suspend fun getReminderSetting(id: Int = 1): ReminderSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderSetting(setting: ReminderSetting)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
