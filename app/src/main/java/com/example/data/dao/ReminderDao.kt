package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import com.example.data.entity.LinkedDevice
import com.example.data.entity.DeviceLog
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

    // --- Linked Devices Queries ---
    @Query("SELECT * FROM linked_devices ORDER BY pairedDate DESC")
    fun getAllDevicesFlow(): Flow<List<LinkedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: LinkedDevice)

    @Query("DELETE FROM linked_devices WHERE id = :id")
    suspend fun deleteDevice(id: String)

    @Query("SELECT * FROM linked_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: String): LinkedDevice?

    // --- External Device Sync Logs Queries ---
    @Query("SELECT * FROM device_logs ORDER BY timestamp DESC")
    fun getAllDeviceLogsFlow(): Flow<List<DeviceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceLog(log: DeviceLog)

    @Query("DELETE FROM device_logs")
    suspend fun clearDeviceLogs()
}

