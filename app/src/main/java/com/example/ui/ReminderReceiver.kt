package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Alarm intent received! Action: ${intent.action}")
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    Log.d("ReminderReceiver", "System reboot complete. Rescheduling wellness alarms.")
                    val db = AppDatabase.getDatabase(context)
                    val setting = db.reminderDao().getReminderSetting()
                    if (setting != null && setting.isActive) {
                        val trigger = if (setting.nextScheduledTime > System.currentTimeMillis()) {
                            setting.nextScheduledTime
                        } else {
                            System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
                        }
                        ReminderAlarmScheduler.scheduleNextAlarm(context, trigger)
                        // Restart foreground service on boot to resist standby priority drops
                        WellnessForegroundService.startService(context)
                    }
                    pendingResult.finish()
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                var setting = db.reminderDao().getReminderSetting() ?: ReminderSetting()
                
                // Reset daily occurrences if calendar day has changed
                val now = System.currentTimeMillis()
                if (setting.lastTriggeredTime > 0L && !isSameDay(setting.lastTriggeredTime, now)) {
                    Log.d("ReminderReceiver", "Calendar day boundary crossed. Resetting occurrences to 0.")
                    setting = setting.copy(completedOccurrencesToday = 0)
                    db.reminderDao().insertReminderSetting(setting)
                }
                
                if (!setting.isActive) {
                    Log.d("ReminderReceiver", "Reminders are currently inactive. Ignoring.")
                    pendingResult.finish()
                    return@launch
                }

                // Check calendar/days
                if (!isTodayRepetitionAllowed(setting.repetitionType)) {
                    Log.d("ReminderReceiver", "Reminders are not allowed today based on repetition style.")
                    // Schedule next
                    rescheduleNext(context, setting, db)
                    pendingResult.finish()
                    return@launch
                }

                // Check maximum counts limit
                if (setting.maxOccurrencesPerDay > 0 && setting.completedOccurrencesToday >= setting.maxOccurrencesPerDay) {
                    Log.d("ReminderReceiver", "Max daily occurrences reached.")
                    // Schedule next
                    rescheduleNext(context, setting, db)
                    pendingResult.finish()
                    return@launch
                }

                // Pick a categories card
                val pool = mutableListOf<String>()
                if (setting.drinkWaterEnabled) pool.add("WATER")
                if (setting.walkEnabled) pool.add("WALK")
                if (setting.stretchEnabled) pool.add("STRETCH")
                if (setting.mindfulEnabled) pool.add("MINDFUL")
                
                val chosenCategory = if (pool.isEmpty()) "STRETCH" else pool.random()

                // Trigger Notification
                triggerSystemNotification(context, chosenCategory)

                // Trigger Sound and Vibe!
                playSystemSoundAndVibe(context, setting.alertSound, setting.vibrationPattern)

                // Log this in Activity Logs
                val notes = when (chosenCategory) {
                    "WATER" -> "Drank water. Hydrated and refreshed!"
                    "WALK" -> "Took a light walk. Reset lower-body circulation."
                    "STRETCH" -> "Completed desk stretch. Relieved upper-body tension!"
                    "MINDFUL" -> "Completed mindful deep breathing. Reset mental clarity."
                    else -> "Completed wellness exercise."
                }
                db.reminderDao().insertLog(
                    ActivityLog(
                        activityType = chosenCategory,
                        notes = notes
                    )
                )

                // Save setting with updated count & times
                val nextTime = System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
                val updated = setting.copy(
                    completedOccurrencesToday = setting.completedOccurrencesToday + 1,
                    nextScheduledTime = nextTime,
                    lastTriggeredTime = System.currentTimeMillis(),
                    snoozedUntil = 0
                )
                db.reminderDao().insertReminderSetting(updated)

                // Schedule next alarm!
                ReminderAlarmScheduler.scheduleNextAlarm(context, nextTime)

                Log.d("ReminderReceiver", "Reminder executed successfully, scheduled next.")
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isTodayRepetitionAllowed(type: String): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val isWeekday = !isWeekend

        return when (type) {
            "WEEKDAYS" -> isWeekday
            "WEEKENDS" -> isWeekend
            else -> true
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        if (time1 == 0L || time2 == 0L) return true
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun rescheduleNext(context: Context, setting: ReminderSetting, db: AppDatabase) {
        val nextTime = System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
        val updated = setting.copy(
            nextScheduledTime = nextTime,
            snoozedUntil = 0
        )
        CoroutineScope(Dispatchers.IO).launch {
            db.reminderDao().insertReminderSetting(updated)
            ReminderAlarmScheduler.scheduleNextAlarm(context, nextTime)
        }
    }

    private fun triggerSystemNotification(context: Context, category: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "wellness_coaching_alerts"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Wellness Cockpit Alerts"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels reminders for your visual, core muscular and hydration micro-breaks."
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)

        val title = when (category) {
            "WATER" -> "💧 Hydration Sip Required!"
            "WALK" -> "🚶 Time for a Quick Walk!"
            "STRETCH" -> "🧘 Do a quick muscles stretch!"
            "MINDFUL" -> "🌬️ 2-Min Mindful Breath reset!"
            else -> "⏰ Workspace Wellness Micro-Break!"
        }

        val text = when (category) {
            "WATER" -> "Keep your hydration level high. Take three slow sips of water right now."
            "WALK" -> "Activate core lower limbs by stepping away from your workspace for 60 seconds."
            "STRETCH" -> "Relax back tension and expand chest muscles with a gentle desk stretch."
            "MINDFUL" -> "Clear your neural slate by regulating your breathing pattern."
            else -> "Step away briefly, take a deep breath, and reset your workspace energy."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(777, builder.build())
    }

    private fun playSystemSoundAndVibe(context: Context, soundName: String, vibePattern: String) {
        if (soundName.equals("Silent", ignoreCase = true)) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val streamType = AudioManager.STREAM_NOTIFICATION
                val toneGenerator = ToneGenerator(streamType, 80)
                when (soundName) {
                    "Muted Chimes" -> {
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 100)
                        delay(120)
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_4, 100)
                        delay(120)
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_7, 100)
                    }
                    "Zen Wood", "Zen Wood Block" -> {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                    }
                    "Cosmic Bell", "Cosmic Resonance" -> {
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 200)
                        delay(250)
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_C, 300)
                    }
                    else -> {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }
                }
                delay(1200)
                toneGenerator.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (vibePattern) {
                        "Soft Pulse" -> {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        "Deep Breathing" -> {
                            val timings = longArrayOf(0, 150, 200, 150, 200, 150)
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                        }
                        "Double Knock" -> {
                            val timings = longArrayOf(0, 60, 100, 60)
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                        }
                        else -> {
                            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    when (vibePattern) {
                        "Soft Pulse" -> vibrator.vibrate(100)
                        "Deep Breathing" -> vibrator.vibrate(longArrayOf(0, 150, 200, 150), -1)
                        "Double Knock" -> vibrator.vibrate(longArrayOf(0, 60, 100, 60), -1)
                        else -> vibrator.vibrate(150)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
