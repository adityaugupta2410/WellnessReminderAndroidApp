package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ReminderDao
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import com.example.data.entity.LinkedDevice
import com.example.data.entity.DeviceLog

@Database(entities = [ReminderSetting::class, ActivityLog::class, LinkedDevice::class, DeviceLog::class], version = 4, exportSchema = false)

abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellness_reminder_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
