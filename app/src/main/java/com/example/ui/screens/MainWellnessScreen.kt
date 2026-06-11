package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ActivityLog
import com.example.data.entity.ReminderSetting
import com.example.ui.ReminderViewModel
import com.example.ui.model.FactItem
import com.example.ui.model.StretchItem
import com.example.ui.model.MindfulExercise
import com.example.ui.model.WellnessData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.PowerManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainWellnessScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val setting by viewModel.reminderSetting.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isAlertShowing by viewModel.isAlertShowing.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val simulatedNotificationText by viewModel.simulatedNotificationText.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    var showSplash by remember { mutableStateOf(true) }
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        InitialSplashAG()
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Schedule Settings") },
                        label = { Text("Schedule", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_schedule")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Stretches & Facts") },
                        label = { Text("Wellness Spot", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_stretches")
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                        label = { Text("Logs", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_logs")
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Insights") },
                        label = { Text("Insights", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_insights")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    0 -> DashboardTab(viewModel, setting, timeLeftSeconds, isTimerRunning, onProfileClick = { showProfileSheet = true })
                    1 -> ScheduleTab(viewModel, setting)
                    2 -> StretchesTab(viewModel)
                    3 -> LogsTab(viewModel, logs)
                    4 -> InsightsTab(viewModel, logs)
                }

                if (showProfileSheet) {
                    ProfileScreen(viewModel = viewModel, onDismiss = { showProfileSheet = false })
                }

                AnimatedVisibility(
                    visible = simulatedNotificationText != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                ) {
                    simulatedNotificationText?.let { info ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(info, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // High Priority Interactive Overlay Dialog for dynamic alerts
                if (isAlertShowing) {
                    val category by viewModel.activeAlertCategory.collectAsState()
                    val fact by viewModel.activeFact.collectAsState()
                    val stretch by viewModel.activeStretch.collectAsState()
                    val mindfulExercise by viewModel.activeMindfulExercise.collectAsState()

                    WellnessBreakOverlay(
                        category = category,
                        fact = fact,
                        stretch = stretch,
                        mindfulExercise = mindfulExercise,
                        onComplete = { viewModel.completeReminder(category) },
                        onSnooze = { mins -> viewModel.snoozeReminder(mins) },
                        onDismiss = { viewModel.dismissAlert() }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: ReminderViewModel,
    setting: ReminderSetting?,
    timeLeftSeconds: Int,
    isTimerRunning: Boolean,
    onProfileClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // App Greeting Row with Profile Action
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DESK VITALITY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "Your Workspace Wellness Advocate",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.testTag("dashboard_profile_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "My Profile Hub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Timer Dial Display
        item {
            if (setting != null) {
                DeskHabitTimerDial(
                    timeLeftSeconds = timeLeftSeconds,
                    totalSeconds = setting.frequencyMinutes * 60,
                    isActive = setting.isActive,
                    onToggleActive = { viewModel.toggleActiveState(!setting.isActive) }
                )
            } else {
                CircularProgressIndicator()
            }
        }

        // Daily Break Tracker and Sitting Score Card
        item {
            if (setting != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val progressRatio = if (setting.maxOccurrencesPerDay > 0) {
                        setting.completedOccurrencesToday.toFloat() / setting.maxOccurrencesPerDay.toFloat()
                    } else {
                        1f
                    }
                    
                    // Daily Completed card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Breaks Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${setting.completedOccurrencesToday} / ${setting.maxOccurrencesPerDay}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Strain Index Card
                    val hoursSittingSinceBreak = if (timeLeftSeconds > 0) {
                        // calculate based on elapsed
                        val total = (setting.frequencyMinutes * 60) - timeLeftSeconds
                        total.toFloat() / 3600f
                    } else 0f

                    val strainScore = when {
                        !setting.isActive -> 0
                        hoursSittingSinceBreak > 1.5f -> 85
                        hoursSittingSinceBreak > 1.0f -> 60
                        else -> 25
                    }

                    val strainLabel = when {
                        strainScore >= 80 -> "CRITICAL TENSION"
                        strainScore >= 50 -> "MEDIUM STRAIN"
                        else -> "NORMAL REFRESHED"
                    }

                    val strainColor = when {
                        strainScore >= 80 -> MaterialTheme.colorScheme.error
                        strainScore >= 50 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = strainColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strainLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = strainColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Strain Score: $strainScore",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Quick 1-Tap Trigger Section (UX Focused - lets them click to stretch/hydrate instantly!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant Solitary Breaks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Water Button
                        Button(
                            onClick = { viewModel.triggerManualReminder("WATER") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("trigger_quick_water"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Drink Water", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Quick Stretch Button
                        Button(
                            onClick = { viewModel.triggerManualReminder("STRETCH") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            modifier = Modifier.weight(1f).testTag("trigger_quick_stretch"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Stretch Now", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Quick Walk Button
                        Button(
                            onClick = { viewModel.triggerManualReminder("WALK") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            modifier = Modifier.weight(1f).testTag("trigger_quick_walk"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Take Walk", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeskHabitTimerDial(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    isActive: Boolean,
    onToggleActive: () -> Unit
) {
    val sweepAngle = if (isActive && totalSeconds > 0) {
        (timeLeftSeconds.toFloat() / totalSeconds.toFloat()) * 360f
    } else {
        360f
    }

    // Color animations
    val dialColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val progressColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(240.dp)
            .clickable { onToggleActive() }
            .testTag("timer_dial_toggle"),
        contentAlignment = Alignment.Center
    ) {
        // Dial Background Circle and Progress Bar
        Canvas(modifier = Modifier.size(220.dp)) {
            // Track
            drawCircle(
                color = dialColor.copy(alpha = 0.12f),
                style = Stroke(width = 16.dp.toPx())
            )
            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner stats
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isActive) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap to Resume Monitoring",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            } else if (timeLeftSeconds <= 0) {
                Text(
                    text = "DUE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Preparing Break Advisory",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val mins = timeLeftSeconds / 60
                val secs = timeLeftSeconds % 60
                val timeString = String.format("%02d:%02d", mins, secs)

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Next Break",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ScheduleTab(
    viewModel: ReminderViewModel,
    setting: ReminderSetting?
) {
    if (setting == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var infoDialogContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schedule Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = {
                        infoDialogContent = Pair(
                            "Schedule Assistant",
                            "Welcome to your smart wellness cockpit. Here you can configure how and when you receive health and energy resets. Click the small 'i' info icons throughout settings to see scientific micro-break advice."
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About Configurations",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 1-Click Setup Templates Component (Beautiful and Visual!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1-Click Focus Presets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                infoDialogContent = Pair(
                                    "Focus Presets",
                                    "Quickly tune the wellness cycle frequency based on your desk demands:\n\n• High Focus (30 min): Best for intensive high-tension tasks.\n• Balanced (45 min): Highly recommended desk rhythm.\n• Standard (60 min): Classic light pacing."
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Preset Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetButton(
                            title = "High Focus",
                            minutes = 30,
                            icon = Icons.Default.Lock,
                            currentMinutes = setting.frequencyMinutes,
                            onClick = { viewModel.updateFrequency(30) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            title = "Balanced",
                            minutes = 45,
                            icon = Icons.Default.Star,
                            currentMinutes = setting.frequencyMinutes,
                            onClick = { viewModel.updateFrequency(45) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            title = "Standard",
                            minutes = 60,
                            icon = Icons.Default.CheckCircle,
                            currentMinutes = setting.frequencyMinutes,
                            onClick = { viewModel.updateFrequency(60) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Duration Adjustment Slider Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Interval Frequency",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    infoDialogContent = Pair(
                                        "Interval Frequency",
                                        "Determine the exact spacing between wellness breaks.\n\nMicro-cues work best when scheduled every 45 to 60 minutes to regularly stretch visual focus, reset ocular convergence, and stretch core muscle systems."
                                    )
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${setting.frequencyMinutes} Min Cycle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = setting.frequencyMinutes.toFloat(),
                        onValueChange = { viewModel.updateFrequency(it.toInt()) },
                        valueRange = 10f..120f,
                        steps = 22,
                        modifier = Modifier.testTag("frequency_slider")
                    )
                }
            }
        }

        // Category Selections (Water, Walk, Stretch, Mindful with premium Switches!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enabled Break Categories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                infoDialogContent = Pair(
                                    "Break Categories",
                                    "The app will intelligently alternate between your enabled focus break categories. Toggle off categories you don't wish to track today."
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Break categories overview",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Water toggle with Switch
                    CategoryToggleRow(
                        title = "Water Hydration Cues",
                        icon = Icons.Default.Refresh,
                        checked = setting.drinkWaterEnabled,
                        testTag = "toggle_water",
                        onCheckedChange = { viewModel.updateToggles(it, setting.walkEnabled, setting.stretchEnabled, setting.mindfulEnabled) },
                        onInfoClick = {
                            infoDialogContent = Pair(
                                "Water Intake Cues",
                                "Scientific studies show that even 1% dehydration leads to immediate drops in short-term storage memory and increased workplace brain fog. Consistent sipping resets physical fluid balance."
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Walk toggle with Switch
                    CategoryToggleRow(
                        title = "Circulation Walk Cues",
                        icon = Icons.Default.Star,
                        checked = setting.walkEnabled,
                        testTag = "toggle_walk",
                        onCheckedChange = { viewModel.updateToggles(setting.drinkWaterEnabled, it, setting.stretchEnabled, setting.mindfulEnabled) },
                        onInfoClick = {
                            infoDialogContent = Pair(
                                "Circulation Walk Breaks",
                                "Standing up or taking a light walk for 1-2 minutes stimulates critical vascular lower-body circulation, releases lower lumbar disks from continuous weight, and triggers mental resets."
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Stretch toggle with Switch
                    CategoryToggleRow(
                        title = "Ergonomic Stretch Cues",
                        icon = Icons.Default.PlayArrow,
                        checked = setting.stretchEnabled,
                        testTag = "toggle_stretch",
                        onCheckedChange = { viewModel.updateToggles(setting.drinkWaterEnabled, setting.walkEnabled, it, setting.mindfulEnabled) },
                        onInfoClick = {
                            infoDialogContent = Pair(
                                "Ergonomic Stretching",
                                "Predefined office-safe stretches that require no equipment or physical layout changes. Recharges muscle fibers and limits systemic postural strain."
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Mindful toggle with Switch
                    CategoryToggleRow(
                        title = "Mindfulness Reset Cues",
                        icon = Icons.Default.Favorite,
                        checked = setting.mindfulEnabled,
                        testTag = "toggle_mindful",
                        onCheckedChange = { viewModel.updateToggles(setting.drinkWaterEnabled, setting.walkEnabled, setting.stretchEnabled, it) },
                        onInfoClick = {
                            infoDialogContent = Pair(
                                "Mindfulness & Eye Resets",
                                "Guided box breathing pacing combined with visual decompress steps. Gently balances sympathetic stress systems and relaxes optometric nerves."
                            )
                        }
                    )
                }
            }
        }

        // Customizable alert sounds and vibrator feedback selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chimes & Tactile Pulses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                infoDialogContent = Pair(
                                    "Chimes & Vibrations",
                                    "Customize your alert sound and tactile heartbeat patterns.\n\nSelecting 'Zen Wood Block' or 'Cosmic Resonance' triggers a brief simulated sound playback so you can instantly evaluate the signature."
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Chimes info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound options row chips
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alert Chime Selection",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val sounds = listOf("Muted Chimes", "Zen Wood", "Cosmic Bell", "Silent")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sounds.forEach { snd ->
                            val isChosen = setting.alertSound == snd || (snd == "Zen Wood" && setting.alertSound == "Zen Wood Block") || (snd == "Cosmic Bell" && setting.alertSound == "Cosmic Resonance")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    )
                                    .clickable { 
                                        val fullSoundName = when (snd) {
                                            "Zen Wood" -> "Zen Wood Block"
                                            "Cosmic Bell" -> "Cosmic Resonance"
                                            else -> snd
                                        }
                                        viewModel.updateAlertSound(fullSoundName) 
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = snd,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vibration options row chips
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vibration Pulse Signature",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val vibrations = listOf("Soft Pulse", "Steady Sync", "Continuous Wave", "No Vibration")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            vibrations.take(2).forEach { vib ->
                                val isChosen = setting.vibrationPattern == vib
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isChosen) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        )
                                        .clickable { viewModel.updateVibrationPattern(vib) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = vib,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            vibrations.drop(2).forEach { vib ->
                                val isChosen = setting.vibrationPattern == vib
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isChosen) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        )
                                        .clickable { viewModel.updateVibrationPattern(vib) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = vib,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Repetitions days scheduling
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Schedule Days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                infoDialogContent = Pair(
                                    "Active Days",
                                    "Toggle how automatically scheduled breaks run during the week. This keeps notifications quiescent on non-work days so you are never disturbed during personal off-hours."
                                )
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Schedule days info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepetitionChoiceButton(
                            label = "Weekdays Only",
                            isSelected = setting.repetitionType == "WEEKDAYS",
                            onClick = { viewModel.updateRepetition("WEEKDAYS", setting.maxOccurrencesPerDay) },
                            modifier = Modifier.weight(1f)
                        )
                        RepetitionChoiceButton(
                            label = "Every Day",
                            isSelected = setting.repetitionType == "EVERYDAY",
                            onClick = { viewModel.updateRepetition("EVERYDAY", setting.maxOccurrencesPerDay) },
                            modifier = Modifier.weight(1f)
                        )
                        RepetitionChoiceButton(
                            label = "Weekends Only",
                            isSelected = setting.repetitionType == "WEEKENDS",
                            onClick = { viewModel.updateRepetition("WEEKENDS", setting.maxOccurrencesPerDay) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Repetition occurrence limit (counts per day)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Break Cap",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    infoDialogContent = Pair(
                                        "Daily Break Cap",
                                        "Set a hard ceiling on total wellness alerts triggered per day.\n\nOnce reached, tracking halts until the next morning to ensure the system is completely non-intrusive."
                                    )
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Breaks cap info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${setting.maxOccurrencesPerDay} Breaks / Day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = setting.maxOccurrencesPerDay.toFloat(),
                        onValueChange = { viewModel.updateRepetition(setting.repetitionType, it.toInt()) },
                        valueRange = 2f..16f,
                        steps = 13,
                        modifier = Modifier.testTag("max_repeats_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.resetDailyOccurrences() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Completed Counter", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Interactive Accent Color Theme Selector block
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_selection_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wellness Accent Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Customize the visual appearance and energy tone of the Wellness Cockpit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val themesList = listOf(
                        Triple("LAVENDER", "Luminous Lavender (Default)", Color(0xFFD0BCFF)),
                        Triple("EMERALD", "Emerald Mint", Color(0xFF81C784)),
                        Triple("OCEAN", "Cosmic Blue", Color(0xFF80DEEA)),
                        Triple("ROSE", "Sunset Rose", Color(0xFFEF9A9A))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themesList.forEach { (themeId, label, color) ->
                            val isChosen = setting?.themeName == themeId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateThemeName(themeId) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("theme_option_$themeId"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChosen) FontWeight.Black else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Samsung & Active Standby Sleeping Apps Troubleshooting Card
        item {
            val context = LocalContext.current
            var batteryStatusText by remember { mutableStateOf("Checking battery optimization...") }
            var isBatteryExempted by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                try {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (powerManager != null) {
                        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            powerManager.isIgnoringBatteryOptimizations(context.packageName)
                        } else {
                            true
                        }
                        isBatteryExempted = isIgnoring
                        batteryStatusText = if (isIgnoring) {
                            "🛡️ Battery Optimizations: EXEMPTED (Active reminders will resist background sleep)"
                        } else {
                            "⚠️ Battery Optimizations: OPTIMIZED (Reminders may be put to sleep by the system)"
                        }
                    }
                } catch (e: Exception) {
                    batteryStatusText = "Status check unavailable"
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("samsung_battery_optim_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBatteryExempted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isBatteryExempted) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isBatteryExempted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Samsung S23/S24 & Background Sleep Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryExempted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Samsung Devices (One UI 5/6+) are extremely aggressive and will force Wellness Cockpit background notifications to sleep after a few hours unless explicitly configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🔧 Recommended Samsung Setup:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Tap 'Open App Settings' below -> select Battery -> set to 'Unrestricted'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Select 'Alarms & Reminders' on the same page -> ensure toggle is Allowed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Keep 'Wellness Cockpit' active list. Do not place in 'Deep Sleeping Apps' in Device Care settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBatteryExempted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = batteryStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryExempted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button to open the direct App Info screen (Samsung details)
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBatteryExempted) MaterialTheme.colorScheme.secondary 
                                else MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open App Settings", style = MaterialTheme.typography.labelSmall)
                        }

                        // Button to request Direct Battery Exemption Prompt
                        if (!isBatteryExempted) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            val intent = Intent().apply {
                                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exempt Battery", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // High fidelity Material 3 Dialog for clean "i" information popups
    if (infoDialogContent != null) {
        AlertDialog(
            onDismissRequest = { infoDialogContent = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { 
                Text(
                    text = infoDialogContent!!.first, 
                    fontWeight = FontWeight.Bold, 
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = { 
                Text(
                    text = infoDialogContent!!.second, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = { infoDialogContent = null }
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun CategoryToggleRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = { onInfoClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag).scale(0.85f)
        )
    }
}

@Composable
fun PresetButton(
    title: String,
    minutes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = currentMinutes == minutes
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier.height(44.dp).testTag("preset_$minutes"),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun RepetitionChoiceButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier.aspectRatio(1.1f).testTag("repeat_$label")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(22.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StretchesTab(viewModel: ReminderViewModel) {
    var chosenPreviewStretch by remember { mutableStateOf<StretchItem?>(null) }

    if (chosenPreviewStretch != null) {
        // Active Stretch Guidance Coach
        StretchGuidedCoachScreen(
            stretch = chosenPreviewStretch!!,
            onBack = { chosenPreviewStretch = null }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Safe Seated Stretches",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Professional stretches selected to never cause joint pressure or awkward movements in an office desk layout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(WellnessData.stretches) { stretch ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { chosenPreviewStretch = stretch }
                        .testTag("stretch_card_${stretch.title.replace(" ", "_")}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stretch.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = stretch.difficulty,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stretch.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target: ${stretch.targetArea}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Start Session (${stretch.durationSeconds}s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Scientifically Backed Break Value",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A simple 30-second chest extension resets shoulder posture from keyboard hunching and boosts blood flow to your brain by up to 22%. Your physical health is your professional edge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StretchGuidedCoachScreen(
    stretch: StretchItem,
    onBack: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var currentStepIndex by remember { mutableStateOf(0) }
    var isRepPlaying by remember { mutableStateOf(true) }
    
    val stepDuration = stretch.durationSeconds / stretch.steps.size
    var secondsLeft by remember { mutableStateOf(stepDuration) }

    var repCount by remember { mutableStateOf(1) }

    LaunchedEffect(currentStepIndex, isRepPlaying) {
        if (!isRepPlaying) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            if (isRepPlaying) {
                secondsLeft -= 1
            }
        }
        if (currentStepIndex < stretch.steps.size - 1) {
            currentStepIndex += 1
            secondsLeft = stepDuration
            repCount += 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("guided_coach_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("coach_back_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Return")
            }
            Text(
                text = "HD COACH PLAYER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Box(modifier = Modifier.size(48.dp))
        }

        // --- EXERCISE STREAM VIDEO PLAYER (16:9 Aspect Ratio) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.77f)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .shadow(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                StretchVideoDemonstrator(
                    stretchTitle = stretch.title,
                    isPlaying = isRepPlaying,
                    modifier = Modifier.fillMaxSize()
                )

                // Video Info overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Red,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE LOOP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        Text(
                            text = "1080P HD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Play / Pause central indicator overlay
                if (!isRepPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .padding(8.dp)
                        )
                    }
                }

                // Video player timeline & controls bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val progressFraction = if (stepDuration > 0) {
                            (stepDuration - secondsLeft).toFloat() / stepDuration.toFloat()
                        } else {
                            0f
                        }
                        
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { isRepPlaying = !isRepPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRepPlaying) {
                                        Row(
                                            modifier = Modifier.size(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(Color.White))
                                            Box(modifier = Modifier.fillMaxHeight().width(2.5.dp).background(Color.White))
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                val elapsed = stepDuration - secondsLeft
                                Text(
                                    text = "0:${String.format("%02d", elapsed)} / 0:${String.format("%02d", stepDuration)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "AUTO-COACH FEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // --- CONCISE REPETITION & HOLDS COUNTER DECK ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LIVE MOTION TRACKER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REPETITION: $repCount OF ${stretch.steps.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val circleProgress = repCount.toFloat() / stretch.steps.size.toFloat()
                    CircularProgressIndicator(
                        progress = { circleProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$secondsLeft",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "secs",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- STEP INSTRUCTIONS DECK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STEP ${currentStepIndex + 1} DIRECTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stretch.steps[currentStepIndex],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inhale deep; synchronize your breath to the video motion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (currentStepIndex > 0) {
                        currentStepIndex -= 1
                        secondsLeft = stepDuration
                        repCount = currentStepIndex + 1
                    }
                },
                enabled = currentStepIndex > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Previous Rep")
            }

            Button(
                onClick = {
                    if (currentStepIndex < stretch.steps.size - 1) {
                        currentStepIndex += 1
                        secondsLeft = stepDuration
                        repCount = currentStepIndex + 1
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f).testTag("coach_next_or_done"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (currentStepIndex == stretch.steps.size - 1) "Complete" else "Next Rep")
            }
        }
    }
}

@Composable
fun LogsTab(
    viewModel: ReminderViewModel,
    logs: List<ActivityLog>
) {
    var statsRangeTab by remember { mutableStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                     text = "Wellness Insights",
                     style = MaterialTheme.typography.headlineSmall,
                     fontWeight = FontWeight.Black,
                     color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                     text = "Review your consistency analytics.",
                     style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (logs.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearAllLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Stats Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Range Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Daily", "Weekly Graph", "Monthly Index").forEachIndexed { index, label ->
                        val isSel = statsRangeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { statsRangeTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeframe Calculation
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val filteredLogs = when (statsRangeTab) {
                    0 -> logs.filter { it.timestamp >= startOfToday }
                    1 -> logs.filter { it.timestamp >= startOfWeek }
                    else -> logs.filter { it.timestamp >= startOfMonth }
                }

                val categoryCounts = mapOf(
                    "WATER" to filteredLogs.count { it.activityType == "WATER" },
                    "WALK" to filteredLogs.count { it.activityType == "WALK" },
                    "STRETCH" to filteredLogs.count { it.activityType == "STRETCH" },
                    "MINDFUL" to filteredLogs.count { it.activityType == "MINDFUL" }
                )

                if (logs.isEmpty()) {
                    Text(
                        text = "Complete reminder actions to populate stats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    when (statsRangeTab) {
                        0 -> { // Daily Layout
                            Text(
                                text = "TODAY'S CONSISTENCY BREAKDOWN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal Segment Stack Progress Bar representation
                            val totalDaily = categoryCounts.values.sum().coerceAtLeast(1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                categoryCounts.forEach { (cat, count) ->
                                    val ratio = count.toFloat() / totalDaily
                                    if (ratio > 0f) {
                                        val color = when (cat) {
                                            "WATER" -> MaterialTheme.colorScheme.primary
                                            "WALK" -> MaterialTheme.colorScheme.tertiary
                                            "STRETCH" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(ratio)
                                                .background(color)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Category Legend Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                mapOf(
                                    "Water" to Pair(categoryCounts["WATER"] ?: 0, MaterialTheme.colorScheme.primary),
                                    "Walk" to Pair(categoryCounts["WALK"] ?: 0, MaterialTheme.colorScheme.tertiary),
                                    "Stretch" to Pair(categoryCounts["STRETCH"] ?: 0, MaterialTheme.colorScheme.onSecondaryContainer),
                                    "Mindful" to Pair(categoryCounts["MINDFUL"] ?: 0, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ).forEach { (label, data) ->
                                    val (count, color) = data
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${count}x", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }

                        1 -> { // Weekly Column Bar Chart
                            Text(
                                text = "WEEKLY ACTIVITY GRAPH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Compute counts per weekday
                            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val dayCounts = IntArray(7)
                            val cal = Calendar.getInstance()
                            val weekLogs = logs.filter { it.timestamp >= startOfWeek }
                            weekLogs.forEach { log ->
                                cal.timeInMillis = log.timestamp
                                val day = cal.get(Calendar.DAY_OF_WEEK)
                                val index = when (day) {
                                    Calendar.MONDAY -> 0
                                    Calendar.TUESDAY -> 1
                                    Calendar.WEDNESDAY -> 2
                                    Calendar.THURSDAY -> 3
                                    Calendar.FRIDAY -> 4
                                    Calendar.SATURDAY -> 5
                                    Calendar.SUNDAY -> 6
                                    else -> 0
                                }
                                if (log.activityType != "SNOOZE") {
                                    dayCounts[index]++
                                }
                            }

                            val maxCount = dayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                            Row(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                dayNames.forEachIndexed { i, name ->
                                    val count = dayCounts[i]
                                    val ratio = count.toFloat() / maxCount
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight((ratio * 0.7f).coerceAtLeast(0.08f))
                                                .width(16.dp)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (count > 0) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        2 -> { // Monthly Consistency Gauge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val completedInMonth = filteredLogs.count { it.activityType != "SNOOZE" }
                                val monthTarget = 120
                                val complRatio = (completedInMonth.toFloat() / monthTarget).coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = complRatio,
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 6.dp,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "${(complRatio * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = "MONTHLY COMPLIANCE INDEX",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${completedInMonth} of ${monthTarget} target breaks catalogued",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Aim for 4 breaks per day on weekdays to hit peak metabolic health.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recorded activity yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Your break events, snoozes, and stretching activities will appear here to motivate your physical wellness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("logs_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (color, icon) = when (log.activityType) {
                                "WATER" -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Refresh)
                                "WALK" -> Pair(MaterialTheme.colorScheme.tertiary, Icons.Default.Star)
                                "STRETCH" -> Pair(MaterialTheme.colorScheme.onSecondaryContainer, Icons.Default.PlayArrow)
                                "MINDFUL" -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Icons.Default.Favorite)
                                else -> Pair(MaterialTheme.colorScheme.outlineVariant, Icons.Default.Info)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy · H:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsTab(
    viewModel: ReminderViewModel,
    logs: List<ActivityLog>
) {
    var waterLoggedInput by remember { mutableStateOf(4) } // Default 4 glasses
    var waterDateOffset by remember { mutableStateOf(0) } // 0 = Today, 1 = Yesterday, 2 = 2 Days Ago, 3 = 3 Days Ago
    var logSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Core aggregation arithmetic
    val aggregatedDays = remember(logs) {
        val map = mutableMapOf<String, DayAggregate>()
        val cal = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dfLabel = SimpleDateFormat("MMM d", Locale.getDefault())
        
        logs.forEach { log ->
            cal.timeInMillis = log.timestamp
            val dateString = df.format(cal.time)
            val dateLabel = dfLabel.format(cal.time)
            
            val existing = map[dateString] ?: DayAggregate(
                dateString = dateString,
                timestamp = log.timestamp,
                dateLabel = dateLabel,
                waterGlasses = 0,
                walkCount = 0,
                skipCount = 0,
                stretchCount = 0,
                mindfulCount = 0
            )
            
            val newAggregate = when (log.activityType) {
                "WATER" -> {
                    val count = if (log.notes.startsWith("GLASSES:")) {
                        log.notes.removePrefix("GLASSES:").toIntOrNull() ?: 1
                    } else {
                        1
                    }
                    existing.copy(waterGlasses = existing.waterGlasses + count)
                }
                "WALK" -> existing.copy(walkCount = existing.walkCount + 1)
                "SKIP" -> existing.copy(skipCount = existing.skipCount + 1)
                "STRETCH" -> existing.copy(stretchCount = existing.stretchCount + 1)
                "MINDFUL" -> existing.copy(mindfulCount = existing.mindfulCount + 1)
                else -> existing
            }
            map[dateString] = newAggregate
        }
        map.values.sortedByDescending { it.timestamp }
    }

    var selectedDayStr by remember(aggregatedDays) {
        mutableStateOf(aggregatedDays.firstOrNull()?.dateString ?: "")
    }

    val selectedDay = aggregatedDays.find { it.dateString == selectedDayStr } ?: aggregatedDays.firstOrNull()

    // 6 Months Monthly Aggregations
    val monthlyAverages = remember(aggregatedDays) {
        val dfYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val dfMonthLabel = SimpleDateFormat("MMM", Locale.getDefault())
        
        // Ensure 6 months representation
        val past6Months = (0..5).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -offset)
            dfYearMonth.format(c.time) to dfMonthLabel.format(c.time)
        }.reversed()
        
        past6Months.map { (yearMonth, label) ->
            val daysInMonth = aggregatedDays.filter { dfYearMonth.format(java.util.Date(it.timestamp)) == yearMonth }
            val avgWater = if (daysInMonth.isNotEmpty()) {
                daysInMonth.map { it.waterGlasses }.average().toFloat()
            } else {
                0f
            }
            val totalWalks = daysInMonth.sumOf { it.walkCount }
            val totalSkips = daysInMonth.sumOf { it.skipCount }
            
            Triple(label, avgWater, totalWalks)
        }
    }

    val hasSufficientData = aggregatedDays.size >= 2

    // Coroutine effect for timing out logged banner
    LaunchedEffect(logSuccessMessage) {
        if (logSuccessMessage != null) {
            delay(3000)
            logSuccessMessage = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading block
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trends & Hydration",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Analyze your 6-month wellness insights & water deficit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllLogs() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Clear database", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Hydration Logger form card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Water Intake Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Pick Offset Date
                    Text(
                        text = "When did you drink this water?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val offsets = listOf("Today", "Yesterday", "2 Days Ago", "3 Days Ago")
                        offsets.forEachIndexed { index, label ->
                            val selected = waterDateOffset == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { waterDateOffset = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Glass selection
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Amount of water (glasses):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { if (waterLoggedInput > 1) waterLoggedInput-- },
                                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Less", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(
                                text = "$waterLoggedInput",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { if (waterLoggedInput < 15) waterLoggedInput++ },
                                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "More", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val oneDayMs = 24L * 60 * 60 * 1000
                                val targetTimestamp = System.currentTimeMillis() - (waterDateOffset * oneDayMs)
                                viewModel.logWaterIntake(waterLoggedInput, targetTimestamp)
                                logSuccessMessage = "Successfully recorded $waterLoggedInput glasses for date!"
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Hydration", fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.logWaterIntake(1, System.currentTimeMillis())
                                logSuccessMessage = "Success: Logged 1 glass for Today!"
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 Glass (Now)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AnimatedVisibility(visible = logSuccessMessage != null) {
                        logSuccessMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Insufficient data fallback vs Charts
        if (!hasSufficientData) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Awaiting Wellness Data Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "To generate compliance models, monthly ratios, and trends, the engine requires at least 2 consecutive days of wellness metrics. Alternatively, seed the database with mock history instantly below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Button(
                            onClick = { viewModel.generateSixMonthsMockData() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Populate 6-Month Demo History", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Trend visualization card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "6-Month Hydration & Walk Trends",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Average water (glasses) & total walks per month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "180 Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Drawing Canvas chart
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            val paddingLeft = 35.dp.toPx()
                            val paddingBottom = 25.dp.toPx()
                            val paddingTop = 10.dp.toPx()
                            val paddingRight = 10.dp.toPx()

                            val graphWidth = canvasWidth - paddingLeft - paddingRight
                            val graphHeight = canvasHeight - paddingBottom - paddingTop

                            // Draw horizontal grid lines (max expected water glasses average = 12)
                            val maxVal = 12f
                            val linesCount = 4
                            for (l in 0..linesCount) {
                                val gridY = paddingTop + graphHeight - (l * (graphHeight / linesCount))
                                drawLine(
                                    color = outlineColor.copy(alpha = 0.3f),
                                    start = androidx.compose.ui.geometry.Offset(paddingLeft, gridY),
                                    end = androidx.compose.ui.geometry.Offset(canvasWidth - paddingRight, gridY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Render bars
                            val barGroupCount = monthlyAverages.size
                            if (barGroupCount > 0) {
                                val groupWidth = graphWidth / barGroupCount
                                val barSpacing = 4.dp.toPx()
                                val barWidth = (groupWidth - barSpacing * 3) / 2

                                monthlyAverages.forEachIndexed { index, (monthLabel, avgWater, totalWalks) ->
                                    val groupCenterX = paddingLeft + index * groupWidth + (groupWidth / 2)
                                    
                                    // Water intake bar (Height relative to maxVal)
                                    val waterBarHeight = (avgWater / maxVal) * graphHeight
                                    val waterBarTop = paddingTop + graphHeight - waterBarHeight
                                    val waterBarLeft = groupCenterX - barSpacing - barWidth
                                    
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                                        ),
                                        topLeft = androidx.compose.ui.geometry.Offset(waterBarLeft, waterBarTop),
                                        size = androidx.compose.ui.geometry.Size(barWidth, waterBarHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )

                                    // Walk bar (Scaled: standard reference = 40 walks/month)
                                    val normalizedWalks = (totalWalks.toFloat() / 40f).coerceAtMost(1f)
                                    val walkBarHeight = normalizedWalks * graphHeight
                                    val walkBarTop = paddingTop + graphHeight - walkBarHeight
                                    val walkBarLeft = groupCenterX + barSpacing
                                    
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(secondaryColor, secondaryColor.copy(alpha = 0.5f))
                                        ),
                                        topLeft = androidx.compose.ui.geometry.Offset(walkBarLeft, walkBarTop),
                                        size = androidx.compose.ui.geometry.Size(barWidth, walkBarHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }

                        // Text labels row below Canvas
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 35.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            monthlyAverages.forEach { (label, _, _) ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chart legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(primaryColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Avg Water (Glasses)", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(secondaryColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Walk Activity", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Patterns & Insights panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pattern & Habit Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))

                        // Calc metrics
                        val weekendAverages = remember(aggregatedDays) {
                            val cal = Calendar.getInstance()
                            val weekendDays = aggregatedDays.filter {
                                cal.timeInMillis = it.timestamp
                                val d = cal.get(Calendar.DAY_OF_WEEK)
                                d == Calendar.SATURDAY || d == Calendar.SUNDAY
                            }
                            val weekdayDays = aggregatedDays.filter {
                                cal.timeInMillis = it.timestamp
                                val d = cal.get(Calendar.DAY_OF_WEEK)
                                d != Calendar.SATURDAY && d != Calendar.SUNDAY
                            }
                            val avgWe = if (weekendDays.isNotEmpty()) weekendDays.map { it.waterGlasses }.average() else 0.0
                            val avgWd = if (weekdayDays.isNotEmpty()) weekdayDays.map { it.waterGlasses }.average() else 0.0
                            avgWe to avgWd
                        }

                        val cumulativeDeficit = remember(aggregatedDays) {
                            // Target is 8 glasses per day
                            aggregatedDays.sumOf { (8 - it.waterGlasses).coerceAtLeast(0) }
                        }

                        val completionRatios = remember(aggregatedDays) {
                            val totalCompleted = logs.count { it.activityType in listOf("WATER", "WALK", "STRETCH", "MINDFUL") }
                            val totalSkipped = logs.count { it.activityType == "SKIP" }
                            val total = totalCompleted + totalSkipped
                            if (total > 0) {
                                (totalCompleted * 100) / total
                            } else {
                                100
                            }
                        }

                        // Render metrics
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Insights 1: Weekend Dip
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Opacity, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Weekend Hydration Variance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    val weVal = String.format(Locale.getDefault(), "%.1f", weekendAverages.first)
                                    val wdVal = String.format(Locale.getDefault(), "%.1f", weekendAverages.second)
                                    Text(
                                        text = if (weekendAverages.first < weekendAverages.second - 1.0) {
                                            "You drink ${weVal} glasses on weekends vs ${wdVal} glasses on weekdays. Try scheduling active hydration alarms on Saturdays!"
                                        } else {
                                            "Hydration consistency is superb! You maintain stable intake across both weekdays (${wdVal} glasses) and weekends (${weVal} glasses)."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Insights 2: Deficit accumulation
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Cumulative Target Deficit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (cumulativeDeficit > 15) {
                                            "Deficit Warning: You skipped a total of $cumulativeDeficit target glasses of water over the captured period. Place a bottle near your setup!"
                                        } else {
                                            "Keep going! Your hydration gap is minimal, with only $cumulativeDeficit glasses missing from standard target compliance."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Insights 3: Break Completion Ratio
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Active Break Completion Rate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "You successfully interact with and complete $completionRatios% of generated desk health alarms. Outstanding consistency!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Day inspector list
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Wellness Logs Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 7 Days scrollable select horizontal layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val displayDays = aggregatedDays.take(7)
                            displayDays.forEach { record ->
                                val isSelected = record.dateString == selectedDayStr
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedDayStr = record.dateString }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = record.dateLabel.split(" ").getOrNull(0) ?: "Day",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            text = record.dateLabel.split(" ").getOrNull(1) ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Box(modifier = Modifier.size(5.dp).background(if (record.waterGlasses >= 8) Color(0xFF2196F3) else Color.Gray.copy(alpha = 0.5f), CircleShape))
                                            Box(modifier = Modifier.size(5.dp).background(if (record.walkCount > 0) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.5f), CircleShape))
                                        }
                                    }
                                }
                            }
                        }

                        // Day inspector stats summary panel
                        selectedDay?.let { day ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Details: ${day.dateLabel}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        val deficitVal = (8 - day.waterGlasses).coerceAtLeast(0)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(if (deficitVal == 0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (deficitVal == 0) "Target Hydrated!" else "$deficitVal Glass Deficit",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (deficitVal == 0) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Metric breakdown rows
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Water metric
                                        OutlinedCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Hydration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${day.waterGlasses} glasses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Walks metric
                                        OutlinedCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Walking", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${day.walkCount} sessions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Skipped break metric
                                        OutlinedCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Skips/Dismiss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${day.skipCount} breaks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Walks Step Equivalent & Active Time text
                                    Text(
                                        text = "* Walk exercises represent ~${day.walkCount * 750} steps and ${day.walkCount * 10} minutes of metabolic activity breaks.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Support definitions for aggregations
data class DayAggregate(
    val dateString: String,
    val timestamp: Long,
    val dateLabel: String,
    val waterGlasses: Int,
    val walkCount: Int,
    val skipCount: Int,
    val stretchCount: Int,
    val mindfulCount: Int
) {
    fun mindfulCountPlusOne(): Int = mindfulCount + 1
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessBreakOverlay(
    category: String,
    fact: FactItem?,
    stretch: StretchItem?,
    mindfulExercise: MindfulExercise?,
    onComplete: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp)
                .testTag("wellness_alert_popup"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Icon Header
                val categoryColor = when (category) {
                    "WATER" -> MaterialTheme.colorScheme.primary
                    "WALK" -> MaterialTheme.colorScheme.tertiary
                    "MINDFUL" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary
                }

                val titleText = when (category) {
                    "WATER" -> "HYDRATE ADVISORY"
                    "WALK" -> "VITAL WALK ALERT"
                    "MINDFUL" -> "MINDFUL RESET MOMENT"
                    else -> "STRETCH MICRO-SESSION"
                }

                val subText = when (category) {
                    "WATER" -> "Time for a small drink of cool water"
                    "WALK" -> "Time to stand up and break sitting stagnation"
                    "MINDFUL" -> "Calm mental session to soothe high-tension stress"
                    else -> "A rapid desk stretch to release body strain"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = categoryColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                // High fidelity triggered premium animation
                when (category) {
                    "WATER" -> WaterDrinkingAnimation()
                    "WALK" -> WalkingAnimation()
                    "MINDFUL" -> MindfulBreathingTriggerAnimation()
                    else -> DeskStretchingAnimation()
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Fact Display component - "shown in an interesting way with some factual data"
                if (fact != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = categoryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WELLNESS EVIDENCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fact.fact,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Source: ${fact.sourceOrStat}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Safe stretches instructions if present
                if (stretch != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stretch.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Easy ${stretch.difficulty} · Target ${stretch.targetArea}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                stretch.steps.take(2).forEachIndexed { index, step ->
                                    Text(
                                        text = "${index + 1}. $step",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (stretch.steps.size > 2) {
                                    Text(
                                        text = "+ ${stretch.steps.size - 2} more steps guided inside Coach",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Mindful Moment instructions if present
                if (mindfulExercise != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = mindfulExercise.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Calm Reset · ${mindfulExercise.durationSeconds}s Session · Cadence: ${mindfulExercise.breathingCadence}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mindfulExercise.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                mindfulExercise.steps.take(3).forEachIndexed { idx, stp ->
                                    Text(
                                        text = "${idx + 1}. $stp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Core Action Button
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("popup_btn_complete"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Break Task Complete", fontWeight = FontWeight.Bold)
                }

                // Elegant Segmented Snooze Row (5, 10, 15m) - 1-Click Snooze UX mandated!
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Need more time? Snooze instantly:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onSnooze(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("snooze_5"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("5 Mins", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { onSnooze(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("snooze_10"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("10 Mins", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { onSnooze(15) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).testTag("snooze_15"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("15 Mins", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InitialSplashAG() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    
    // Scale and alpha animation
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "LogoAlpha"
    )
    
    // Ambient rotating glow
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val backgroundColor = MaterialTheme.colorScheme.background
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        primaryContainerColor.copy(alpha = 0.4f),
                        backgroundColor
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Orbit line
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer { rotationZ = rotation }
        ) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = size.minDimension / 2f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                )
            )
            
            // Orbit node
            drawCircle(
                color = primaryColor.copy(alpha = 0.6f),
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
        ) {
            // Visual monogram "AG"
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor,
                                tertiaryColor
                            )
                        ),
                        CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 48.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            )
                        )
                        Spacer(modifier = Modifier.width((-6).dp))
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 48.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "WELLNESS COCKPIT",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Mindful Micro-Breaks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WaterDrinkingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val cupAngle = when {
        progress < 0.2f -> 0f
        progress < 0.5f -> (progress - 0.2f) / 0.3f * 45f
        progress < 0.8f -> 45f
        else -> (1f - progress) / 0.2f * 45f
    }

    val headAngle = when {
        progress < 0.25f -> 0f
        progress < 0.5f -> (progress - 0.25f) / 0.25f * 15f
        progress < 0.8f -> 15f
        else -> (1f - progress) / 0.2f * 15f
    }

    val waterLevelY = when {
        progress < 0.3f -> 0.4f
        progress < 0.75f -> 0.4f + ((progress - 0.3f) / 0.45f) * 0.5f
        else -> 0.9f
    }

    val throatSwallow = if (progress in 0.35f..0.75f) {
        val cycle = ((progress - 0.35f) * 10f).toInt() % 2
        if (cycle == 0) 1.15f else 0.95f
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = 0.05f),
                radius = size.width / 2f
            )

            // Profile profile head
            rotate(headAngle, pivot = androidx.compose.ui.geometry.Offset(centerX - 20.dp.toPx(), centerY + 40.dp.toPx())) {
                val facePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centerX + 30.dp.toPx(), centerY + 50.dp.toPx())
                    quadraticTo(
                        centerX + 25.dp.toPx() * throatSwallow, centerY + 20.dp.toPx(),
                        centerX + 25.dp.toPx(), centerY - 5.dp.toPx()
                    )
                    lineTo(centerX + 10.dp.toPx(), centerY - 15.dp.toPx())
                    lineTo(centerX + 22.dp.toPx(), centerY - 25.dp.toPx())
                    lineTo(centerX + 20.dp.toPx(), centerY - 32.dp.toPx())
                    lineTo(centerX + 25.dp.toPx(), centerY - 35.dp.toPx())
                    lineTo(centerX + 15.dp.toPx(), centerY - 50.dp.toPx())
                    lineTo(centerX + 35.dp.toPx(), centerY - 55.dp.toPx())
                }

                drawPath(
                    path = facePath,
                    color = Color.Gray.copy(alpha = 0.4f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Cup tilting
            rotate(-cupAngle, pivot = androidx.compose.ui.geometry.Offset(centerX + 15.dp.toPx(), centerY - 20.dp.toPx())) {
                val glassLeft = centerX - 25.dp.toPx()
                val glassRight = centerX + 10.dp.toPx()
                val glassTop = centerY - 40.dp.toPx()
                val glassBottom = centerY + 15.dp.toPx()

                val glassPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(glassLeft, glassTop)
                    lineTo(glassLeft + 5.dp.toPx(), glassBottom)
                    lineTo(glassRight - 5.dp.toPx(), glassBottom)
                    lineTo(glassRight, glassTop)
                }
                drawPath(
                    path = glassPath,
                    color = Color(0xFF2196F3).copy(alpha = 0.5f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )

                val waterPath = androidx.compose.ui.graphics.Path().apply {
                    val dy = glassTop + (glassBottom - glassTop) * waterLevelY
                    moveTo(glassLeft + (5.dp.toPx() * (dy - glassTop) / (glassBottom - glassTop)), dy)
                    quadraticTo(
                        (glassLeft + glassRight) / 2f, dy - 4.dp.toPx() * (1f - waterLevelY),
                        glassRight - (5.dp.toPx() * (dy - glassTop) / (glassBottom - glassTop)), dy
                    )
                    lineTo(glassRight - 5.dp.toPx(), glassBottom)
                    lineTo(glassLeft + 5.dp.toPx(), glassBottom)
                    close()
                }
                drawPath(
                    path = waterPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xBB29B6F6), Color(0xBB0288D1))
                    )
                )

                if (waterLevelY < 0.8f) {
                    val bubbleOffset = (progress * 150f) % 30f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = 2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(glassLeft + 15.dp.toPx(), glassBottom - 5.dp.toPx() - bubbleOffset.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = 1.5f * 1.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(glassRight - 15.dp.toPx(), glassBottom - 15.dp.toPx() - (bubbleOffset * 1.3f).dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun WalkingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val swingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val verticalBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(140.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseLineY = centerY + 35.dp.toPx()

            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = androidx.compose.ui.geometry.Offset(20.dp.toPx(), baseLineY),
                end = androidx.compose.ui.geometry.Offset(size.width - 20.dp.toPx(), baseLineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val pelvisX = centerX
            val pelvisY = centerY + 5.dp.toPx() + verticalBob.dp.toPx()

            val rightLegAngle = java.lang.Math.sin(swingPhase.toDouble()).toFloat() * 25f
            val leftLegAngle = java.lang.Math.sin(swingPhase.toDouble() + java.lang.Math.PI).toFloat() * 25f
            val rightArmAngle = java.lang.Math.sin(swingPhase.toDouble() + java.lang.Math.PI).toFloat() * 18f
            val leftArmAngle = java.lang.Math.sin(swingPhase.toDouble()).toFloat() * 18f

            val rightKneeY = pelvisY + 15.dp.toPx()
            val rightKneeX = pelvisX + java.lang.Math.sin(java.lang.Math.toRadians(rightLegAngle.toDouble())).toFloat() * 15.dp.toPx()
            val rightFootX = rightKneeX + java.lang.Math.sin(java.lang.Math.toRadians((rightLegAngle + 10f).toDouble())).toFloat() * 15.dp.toPx()
            val rightFootY = baseLineY

            val leftKneeY = pelvisY + 15.dp.toPx()
            val leftKneeX = pelvisX + java.lang.Math.sin(java.lang.Math.toRadians(leftLegAngle.toDouble())).toFloat() * 15.dp.toPx()
            val leftFootX = leftKneeX + java.lang.Math.sin(java.lang.Math.toRadians((leftLegAngle + 10f).toDouble())).toFloat() * 15.dp.toPx()
            val leftFootY = baseLineY

            val leftLegPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(pelvisX, pelvisY)
                lineTo(leftKneeX, leftKneeY)
                lineTo(leftFootX, leftFootY)
            }
            drawPath(
                path = leftLegPath,
                color = Color(0xFFA1887F).copy(alpha = 0.6f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )

            val neckX = pelvisX
            val neckY = pelvisY - 25.dp.toPx()
            drawLine(
                color = Color(0xFFEF6C00),
                start = androidx.compose.ui.geometry.Offset(pelvisX, pelvisY),
                end = androidx.compose.ui.geometry.Offset(neckX, neckY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = Color(0xFFE65100),
                radius = 7.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(neckX, neckY - 10.dp.toPx())
            )

            val leftHandX = neckX + java.lang.Math.sin(java.lang.Math.toRadians(leftArmAngle.toDouble())).toFloat() * 16.dp.toPx()
            val leftHandY = neckY + 15.dp.toPx()
            drawLine(
                color = Color(0xFFA1887F).copy(alpha = 0.6f),
                start = androidx.compose.ui.geometry.Offset(neckX, neckY + 3.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(leftHandX, leftHandY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            val rightHandX = neckX + java.lang.Math.sin(java.lang.Math.toRadians(rightArmAngle.toDouble())).toFloat() * 16.dp.toPx()
            val rightHandY = neckY + 15.dp.toPx()
            drawLine(
                color = Color(0xFFEF6C00),
                start = androidx.compose.ui.geometry.Offset(neckX, neckY + 3.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(rightHandX, rightHandY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            val rightLegPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(pelvisX, pelvisY)
                lineTo(rightKneeX, rightKneeY)
                lineTo(rightFootX, rightFootY)
            }
            drawPath(
                path = rightLegPath,
                color = Color(0xFFEF6C00),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun MindfulBreathingTriggerAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val alphaColor by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(140.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            drawCircle(
                color = Color(0xFF00B0FF).copy(alpha = 0.05f),
                radius = (size.width / 2f) * pulseScale
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = alphaColor),
                        Color.Transparent
                    )
                ),
                radius = 45.dp.toPx() * pulseScale,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            val petals = 6
            val radius = 18.dp.toPx() * pulseScale
            for (i in 0 until petals) {
                val angle = i * (360f / petals)
                val petalCenterX = centerX + java.lang.Math.cos(java.lang.Math.toRadians(angle.toDouble())).toFloat() * 12.dp.toPx() * pulseScale
                val petalCenterY = centerY + java.lang.Math.sin(java.lang.Math.toRadians(angle.toDouble())).toFloat() * 12.dp.toPx() * pulseScale
                drawCircle(
                    color = Color(0x9926A69A),
                    radius = radius / 1.5f,
                    center = androidx.compose.ui.geometry.Offset(petalCenterX, petalCenterY)
                )
            }

            drawCircle(
                color = Color(0xFF00796B),
                radius = 10.dp.toPx() * pulseScale,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
        
        Text(
            text = if (pulseScale > 1f) "BREATHE IN" else "BREATHE OUT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.offset(y = 50.dp)
        )
    }
}

@Composable
fun DeskStretchingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(140.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(25.dp.toPx(), centerY + 35.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width - 25.dp.toPx(), centerY + 35.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            rotate(tiltAngle, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY + 20.dp.toPx())) {
                val pelvisX = centerX
                val pelvisY = centerY + 15.dp.toPx()
                val chestX = centerX
                val chestY = centerY - 15.dp.toPx()
                val headX = centerX
                val headY = centerY - 32.dp.toPx()

                drawLine(
                    color = Color(0xAA9E9E9E),
                    start = androidx.compose.ui.geometry.Offset(pelvisX, pelvisY),
                    end = androidx.compose.ui.geometry.Offset(pelvisX - 20.dp.toPx(), pelvisY + 5.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xAA9E9E9E),
                    start = androidx.compose.ui.geometry.Offset(pelvisX, pelvisY),
                    end = androidx.compose.ui.geometry.Offset(pelvisX + 20.dp.toPx(), pelvisY + 5.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF673AB7),
                    start = androidx.compose.ui.geometry.Offset(pelvisX, pelvisY),
                    end = androidx.compose.ui.geometry.Offset(chestX, chestY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color(0xFF512DA8),
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(headX, headY)
                )

                val leftHandX = chestX - 15.dp.toPx() - (tiltAngle * 0.8f).dp.toPx()
                val leftHandY = chestY - 10.dp.toPx() - (3.dp.toPx())
                val rightHandX = chestX + 15.dp.toPx() - (tiltAngle * 0.8f).dp.toPx()
                val rightHandY = chestY - 14.dp.toPx() - (tiltAngle * 1f).dp.toPx()

                drawLine(
                    color = Color(0xFF673AB7),
                    start = androidx.compose.ui.geometry.Offset(chestX, chestY),
                    end = androidx.compose.ui.geometry.Offset(leftHandX, leftHandY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF673AB7),
                    start = androidx.compose.ui.geometry.Offset(chestX, chestY),
                    end = androidx.compose.ui.geometry.Offset(rightHandX, rightHandY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun StretchVideoDemonstrator(
    stretchTitle: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val stretchScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val scaleFactor = if (isPlaying) stretchScale else 0.5f
        
        drawRect(color = Color(0xFF121212))
        
        val scanLines = 15
        for (i in 0 until scanLines) {
            val y = (size.height / scanLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val baseline = centerY + 40.dp.toPx()
        
        when {
            stretchTitle.contains("Neck", ignoreCase = true) -> {
                val tiltAngle = -20f + (scaleFactor * 40f)

                drawLine(
                    color = Color.DarkGray,
                    start = androidx.compose.ui.geometry.Offset(centerX - 30.dp.toPx(), baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX + 30.dp.toPx(), baseline),
                    strokeWidth = 12.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF03A9F4),
                    start = androidx.compose.ui.geometry.Offset(centerX, baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY + 10.dp.toPx()),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round
                )

                rotate(tiltAngle, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY + 10.dp.toPx())) {
                    drawLine(
                        color = Color(0xFFFFB74D),
                        start = androidx.compose.ui.geometry.Offset(centerX, centerY + 10.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(centerX, centerY - 12.dp.toPx()),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = 22.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY - 32.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.5f * (1f - scaleFactor)),
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX - 22.dp.toPx(), centerY - 32.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.5f * scaleFactor),
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX + 22.dp.toPx(), centerY - 32.dp.toPx())
                    )
                }
            }
            stretchTitle.contains("Shoulder", ignoreCase = true) -> {
                val angleRotation = scaleFactor * 360f

                drawLine(
                    color = Color(0xFF424242),
                    start = androidx.compose.ui.geometry.Offset(centerX - 45.dp.toPx(), baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX + 45.dp.toPx(), baseline),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = androidx.compose.ui.geometry.Offset(centerX, baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY - 10.dp.toPx()),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                drawCircle(
                    color = Color(0xFFFFCC80),
                    radius = 16.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY - 26.dp.toPx())
                )

                val leftShoulderX = centerX - 26.dp.toPx()
                val rightShoulderX = centerX + 26.dp.toPx()
                val shoulderY = centerY - 5.dp.toPx()

                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)),
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(leftShoulderX, shoulderY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)),
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(rightShoulderX, shoulderY)
                )

                rotate(angleRotation, pivot = androidx.compose.ui.geometry.Offset(leftShoulderX, shoulderY)) {
                    drawCircle(
                        color = Color(0xFF2E7D32),
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(leftShoulderX + 11.dp.toPx(), shoulderY)
                    )
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = androidx.compose.ui.geometry.Offset(leftShoulderX, shoulderY),
                        end = androidx.compose.ui.geometry.Offset(leftShoulderX + 11.dp.toPx(), shoulderY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                rotate(-angleRotation, pivot = androidx.compose.ui.geometry.Offset(rightShoulderX, shoulderY)) {
                    drawCircle(
                        color = Color(0xFF2E7D32),
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(rightShoulderX + 11.dp.toPx(), shoulderY)
                    )
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = androidx.compose.ui.geometry.Offset(rightShoulderX, shoulderY),
                        end = androidx.compose.ui.geometry.Offset(rightShoulderX + 11.dp.toPx(), shoulderY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            stretchTitle.contains("Wrist", ignoreCase = true) -> {
                val flexOffset = scaleFactor * 18.dp.toPx()

                drawLine(
                    color = Color(0xFF9E9E9E).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(centerX - 70.dp.toPx(), centerY + 10.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX + 10.dp.toPx(), centerY + 10.dp.toPx()),
                    strokeWidth = 14.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFFAB40),
                    start = androidx.compose.ui.geometry.Offset(centerX - 65.dp.toPx(), centerY + 10.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX + 10.dp.toPx(), centerY + 10.dp.toPx()),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                rotate(25f - (scaleFactor * 45f), pivot = androidx.compose.ui.geometry.Offset(centerX + 10.dp.toPx(), centerY + 10.dp.toPx())) {
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = androidx.compose.ui.geometry.Offset(centerX + 10.dp.toPx(), centerY + 10.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(centerX + 35.dp.toPx(), centerY + 10.dp.toPx()),
                        strokeWidth = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(centerX + 35.dp.toPx(), centerY + 10.dp.toPx()))
                }

                drawLine(
                    color = Color(0xBB757575),
                    start = androidx.compose.ui.geometry.Offset(centerX + 22.dp.toPx() - flexOffset/2f, centerY + 35.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX + 22.dp.toPx() + flexOffset/2f, centerY - 12.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            stretchTitle.contains("Back", ignoreCase = true) || stretchTitle.contains("Spinal", ignoreCase = true) -> {
                val rAngle = java.lang.Math.sin(scaleFactor.toDouble() * java.lang.Math.PI).toFloat() * 30f

                drawLine(
                    color = Color.DarkGray,
                    start = androidx.compose.ui.geometry.Offset(centerX - 30.dp.toPx(), baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX + 30.dp.toPx(), baseline),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = Color.DarkGray,
                    start = androidx.compose.ui.geometry.Offset(centerX - 20.dp.toPx(), baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX - 20.dp.toPx(), baseline - 35.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )

                val projOffset = (java.lang.Math.sin(java.lang.Math.toRadians(rAngle.toDouble())) * 24.0).toFloat().dp.toPx()

                drawLine(
                    color = Color(0xFFAB47BC),
                    start = androidx.compose.ui.geometry.Offset(centerX - 25.dp.toPx() + projOffset/2f, centerY - 5.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX + 25.dp.toPx() + projOffset/2f, centerY - 5.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFAB47BC),
                    start = androidx.compose.ui.geometry.Offset(centerX, baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX + projOffset/4f, centerY - 5.dp.toPx()),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFF8E24AA),
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX + projOffset/5f, centerY - 17.dp.toPx())
                )
            }
            stretchTitle.contains("Chest", ignoreCase = true) || stretchTitle.contains("Opener", ignoreCase = true) -> {
                val pulseRadius = 15.dp.toPx() + (scaleFactor * 25.dp.toPx())
                
                drawLine(
                    color = Color(0xFF00ACC1),
                    start = androidx.compose.ui.geometry.Offset(centerX, baseline + 10.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY - 15.dp.toPx()),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFF80DEEA),
                    radius = 13.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY - 27.dp.toPx())
                )

                val leftHandBackX = centerX - 18.dp.toPx() - (scaleFactor * 8.dp.toPx())
                val handY = centerY + 10.dp.toPx()
                drawLine(
                    color = Color(0xFF00838F),
                    start = androidx.compose.ui.geometry.Offset(centerX, centerY - 5.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(leftHandBackX, handY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color(0xFF4DD0E1).copy(alpha = 0.4f * (1f - scaleFactor)),
                    radius = pulseRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY - 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            else -> {
                val bounce = (scaleFactor * 10.dp.toPx())
                drawLine(
                    color = Color.Gray,
                    start = androidx.compose.ui.geometry.Offset(centerX, baseline),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    strokeWidth = 6.dp.toPx()
                )
                drawCircle(
                    color = Color.LightGray,
                    radius = 14.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY - 15.dp.toPx() - bounce)
                )
            }
        }
    }
}

