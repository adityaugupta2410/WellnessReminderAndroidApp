package com.example.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase

class WellnessForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("WellnessForegroundService", "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WellnessForegroundService", "Service onStartCommand action: ${intent?.action}")
        
        if (intent?.action == "STOP_SERVICE") {
            stopForegroundAndFinish()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "wellness_coaching_foreground"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Wellness Background Monitor"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ensures workspace wellness alerts are scheduled precisely on Samsung devices."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 1020, launchIntent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Workspace Wellness Active")
            .setContentText("Keeping you active and hydrated with micro-breaks.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        888,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(888, notification)
                }
            } else {
                startForeground(888, notification)
            }
            Log.d("WellnessForegroundService", "Foreground service started successfully.")
        } catch (e: Exception) {
            Log.e("WellnessForegroundService", "Failed to start foreground service", e)
        }
    }

    private fun stopForegroundAndFinish() {
        Log.d("WellnessForegroundService", "Stopping foreground service")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WellnessForegroundService", "Service onDestroy")
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, WellnessForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d("WellnessForegroundService", "startService companion helper successfully invoked.")
            } catch (e: Exception) {
                Log.e("WellnessForegroundService", "Exception in startService helper", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WellnessForegroundService::class.java).apply {
                action = "STOP_SERVICE"
            }
            try {
                context.startService(intent)
                Log.d("WellnessForegroundService", "stopService companion helper to send STOP_SERVICE successfully invoked.")
            } catch (e: Exception) {
                Log.e("WellnessForegroundService", "Exception in stopService helper", e)
            }
        }
    }
}
