package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import com.example.data.repository.ReminderRepository
import com.example.ui.model.FactItem
import com.example.ui.model.StretchItem
import com.example.ui.model.MindfulExercise
import com.example.ui.model.WellnessData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderViewModel(
    application: Application,
    private val repository: ReminderRepository
) : AndroidViewModel(application) {

    // Main status streams
    val reminderSetting: StateFlow<ReminderSetting?> = repository.reminderSettingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<ActivityLog>> = repository.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active alert/reminder popups inside the app
    private val _isAlertShowing = MutableStateFlow(false)
    val isAlertShowing: StateFlow<Boolean> = _isAlertShowing.asStateFlow()

    private val _activeAlertCategory = MutableStateFlow("STRETCH")
    val activeAlertCategory: StateFlow<String> = _activeAlertCategory.asStateFlow()

    private val _activeStretch = MutableStateFlow<StretchItem?>(null)
    val activeStretch: StateFlow<StretchItem?> = _activeStretch.asStateFlow()

    private val _activeMindfulExercise = MutableStateFlow<MindfulExercise?>(null)
    val activeMindfulExercise: StateFlow<MindfulExercise?> = _activeMindfulExercise.asStateFlow()

    private val _activeFact = MutableStateFlow<FactItem?>(null)
    val activeFact: StateFlow<FactItem?> = _activeFact.asStateFlow()

    // Alert Sound / Vibration Live Feedback stream
    private val _simulatedNotificationText = MutableStateFlow<String?>(null)
    val simulatedNotificationText: StateFlow<String?> = _simulatedNotificationText.asStateFlow()

    // Countdown state (Seconds remaining)
    private val _timeLeftSeconds = MutableStateFlow(0)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(true)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // Create initial config if DB is empty
        viewModelScope.launch {
            var setting = repository.getReminderSetting()
            val now = System.currentTimeMillis()
            if (setting.lastTriggeredTime > 0L && !isSameDay(setting.lastTriggeredTime, now)) {
                setting = setting.copy(completedOccurrencesToday = 0)
                repository.saveReminderSetting(setting)
            }
            if (setting.isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), setting.nextScheduledTime)
                WellnessForegroundService.startService(getApplication())
            }
            startTimer()
        }
    }

    private fun startTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isTimerRunning.value) continue

                val setting = reminderSetting.value ?: continue
                
                // Clear daily break counts across midnight calendar day boundaries
                val now = System.currentTimeMillis()
                if (setting.lastTriggeredTime > 0L && !isSameDay(setting.lastTriggeredTime, now)) {
                    val resetSetting = setting.copy(completedOccurrencesToday = 0)
                    repository.saveReminderSetting(resetSetting)
                    continue
                }

                if (!setting.isActive) {
                    _timeLeftSeconds.value = 0
                    continue
                }

                // Verify scheduling rules (Count limit reached)
                if (setting.completedOccurrencesToday >= setting.maxOccurrencesPerDay && setting.maxOccurrencesPerDay > 0) {
                    _timeLeftSeconds.value = 0
                    continue
                }

                // If snoozed, count down to snooze time
                val targetTime = if (setting.snoozedUntil > now) {
                    setting.snoozedUntil
                } else {
                    setting.nextScheduledTime
                }

                val diffMs = targetTime - now
                if (diffMs <= 0) {
                    // Time's up! Check weekday/weekend settings
                    if (isTodayRepetitionAllowed(setting.repetitionType)) {
                        triggerSystemReminder(setting)
                        // Immediately reschedule the next one to avoid continuous popups
                        rescheduleNext(setting)
                    } else {
                        // Skip/Reschedule
                        rescheduleNext(setting)
                    }
                } else {
                    _timeLeftSeconds.value = (diffMs / 1000).toInt()
                }
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
            else -> true // "EVERYDAY" or any other value
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        if (time1 == 0L || time2 == 0L) return true
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private suspend fun rescheduleNext(setting: ReminderSetting) {
        val nextTime = System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
        val updated = setting.copy(
            nextScheduledTime = nextTime,
            snoozedUntil = 0
        )
        repository.saveReminderSetting(updated)
        if (updated.isActive) {
            ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), nextTime)
        }
    }

    private fun triggerSystemReminder(setting: ReminderSetting) {
        // Figure out which type we should trigger. Pick from enabled types.
        val pool = mutableListOf<String>()
        if (setting.drinkWaterEnabled) pool.add("WATER")
        if (setting.walkEnabled) pool.add("WALK")
        if (setting.stretchEnabled) pool.add("STRETCH")
        if (setting.mindfulEnabled) pool.add("MINDFUL")

        // If nothing is enabled, fall back to stretch as requested in prompt!
        // "Do a little stretch(this can be by default present in the app without configuring)"
        val chosenCategory = if (pool.isEmpty()) "STRETCH" else pool.random()

        triggerManualReminder(chosenCategory)
    }

    fun playSimulatedSoundAndVibe(soundName: String, vibePattern: String) {
        viewModelScope.launch {
            _simulatedNotificationText.value = "🔔 Sound: '$soundName' · 📳 Vibe: '$vibePattern'"
            // Trigger physical tone and vibration in the background!
            playSystemTone(soundName)
            triggerSystemVibe(vibePattern)
            
            delay(3000)
            _simulatedNotificationText.value = null
        }
    }

    private fun playSystemTone(soundName: String) {
        if (soundName.equals("Silent", ignoreCase = true)) return
        
        viewModelScope.launch {
            try {
                val streamType = AudioManager.STREAM_NOTIFICATION
                val toneGenerator = ToneGenerator(streamType, 70) // 70% volume
                
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
    }

    private fun triggerSystemVibe(patternName: String) {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (patternName) {
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
                    when (patternName) {
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

    fun triggerManualReminder(category: String) {
        _activeAlertCategory.value = category
        _activeFact.value = WellnessData.getFactForCategory(category)
        
        if (category == "MINDFUL") {
            _activeMindfulExercise.value = WellnessData.getMindfulExerciseForAlert()
            _activeStretch.value = null
        } else {
            _activeMindfulExercise.value = null
            if (category == "STRETCH" || Math.random() < 0.6) {
                _activeStretch.value = WellnessData.getStretchForAlert()
            } else {
                _activeStretch.value = null
            }
        }

        val setting = reminderSetting.value
        val sound = setting?.alertSound ?: "Muted Chimes"
        val vibe = setting?.vibrationPattern ?: "Soft Pulse"
        playSimulatedSoundAndVibe(sound, vibe)

        _isAlertShowing.value = true
    }

    // Actions
    fun dismissAlert() {
        _isAlertShowing.value = false
    }

    fun completeReminder(category: String) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val newCompleted = setting.completedOccurrencesToday + 1
            
            val notes = when (category) {
                "WATER" -> "Drank water. Hydrated and refreshed!"
                "WALK" -> "Took a light walk. Reset lower-body circulation."
                "STRETCH" -> "Completed '${_activeStretch.value?.title ?: "Desk Stretch"}'. Relieved upper-body tension!"
                "MINDFUL" -> "Completed '${_activeMindfulExercise.value?.title ?: "Mindful Moment"}'. Reset mental clarity & rest rate."
                else -> "Completed wellness exercise."
            }

            // Save log
            repository.logActivity(category, notes)

            // Calculate next trigger
            val nextTime = System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
            val updated = setting.copy(
                completedOccurrencesToday = newCompleted,
                nextScheduledTime = nextTime,
                lastTriggeredTime = System.currentTimeMillis(),
                snoozedUntil = 0
            )

            repository.saveReminderSetting(updated)
            if (updated.isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), nextTime)
            }
            _isAlertShowing.value = false
            startTimer() // reset timer state
        }
    }

    fun snoozeReminder(minutes: Int) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val snoozeMs = minutes * 60 * 1000
            val snoozeTarget = System.currentTimeMillis() + snoozeMs

            repository.logActivity("SNOOZE", "Snoozed wellness break for $minutes minutes.")

            val updated = setting.copy(
                snoozedUntil = snoozeTarget
            )

            repository.saveReminderSetting(updated)
            if (updated.isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), snoozeTarget)
            }
            _isAlertShowing.value = false
            startTimer()
        }
    }

    // Settings adjustments
    fun updateFrequency(minutes: Int) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val nextTime = System.currentTimeMillis() + (minutes * 60 * 1000)
            val updated = setting.copy(
                frequencyMinutes = minutes,
                nextScheduledTime = nextTime,
                snoozedUntil = 0
            )
            repository.saveReminderSetting(updated)
            if (updated.isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), nextTime)
            }
            startTimer()
        }
    }

    fun updateToggles(water: Boolean, walk: Boolean, stretch: Boolean, mindful: Boolean) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val updated = setting.copy(
                drinkWaterEnabled = water,
                walkEnabled = walk,
                stretchEnabled = stretch,
                mindfulEnabled = mindful
            )
            repository.saveReminderSetting(updated)
        }
    }

    fun updateAlertSound(sound: String) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val updated = setting.copy(alertSound = sound)
            repository.saveReminderSetting(updated)
            playSimulatedSoundAndVibe(sound, setting.vibrationPattern)
        }
    }

    fun updateVibrationPattern(pattern: String) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val updated = setting.copy(vibrationPattern = pattern)
            repository.saveReminderSetting(updated)
            playSimulatedSoundAndVibe(setting.alertSound, pattern)
        }
    }

    fun updateRepetition(type: String, maxCount: Int) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val updated = setting.copy(
                repetitionType = type,
                maxOccurrencesPerDay = maxCount
            )
            repository.saveReminderSetting(updated)
        }
    }

    fun toggleActiveState(isActive: Boolean) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val nextTime = if (setting.nextScheduledTime > System.currentTimeMillis()) {
                setting.nextScheduledTime
            } else {
                System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
            }
            val updated = setting.copy(isActive = isActive, nextScheduledTime = nextTime)
            repository.saveReminderSetting(updated)
            if (isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), nextTime)
                WellnessForegroundService.startService(getApplication())
            } else {
                ReminderAlarmScheduler.cancelAlarm(getApplication())
                WellnessForegroundService.stopService(getApplication())
            }
        }
    }

    fun updateThemeName(themeName: String) {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val updated = setting.copy(themeName = themeName)
            repository.saveReminderSetting(updated)
        }
    }

    fun resetDailyOccurrences() {
        viewModelScope.launch {
            val setting = repository.getReminderSetting()
            val nextTime = System.currentTimeMillis() + (setting.frequencyMinutes * 60 * 1000)
            val updated = setting.copy(
                completedOccurrencesToday = 0,
                nextScheduledTime = nextTime,
                snoozedUntil = 0
            )
            repository.saveReminderSetting(updated)
            if (updated.isActive) {
                ReminderAlarmScheduler.scheduleNextAlarm(getApplication(), nextTime)
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}

// Factory to inject repository and application context
class ReminderViewModelFactory(
    private val application: Application,
    private val repository: ReminderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
