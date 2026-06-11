package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.DeviceLog
import com.example.data.entity.LinkedDevice
import com.example.ui.ImportState
import com.example.ui.ReminderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ReminderViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val devices by viewModel.linkedDevices.collectAsState()
    val adminLogs by viewModel.deviceLogs.collectAsState()
    val importState by viewModel.documentImportState.collectAsState()

    // Mock pairing animation state
    var isPairingLoading by remember { mutableStateOf(false) }
    var pairingDeviceName by remember { mutableStateOf("") }
    var pairingDeviceType by remember { mutableStateOf("") }

    // Admin unlocked state
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var adminPinEntered by remember { mutableStateOf("") }
    var isAdminUnlocked by remember { mutableStateOf(false) }
    var isPinIncorrect by remember { mutableStateOf(false) }

    // Custom device pairing states
    var customDeviceNameInput by remember { mutableStateOf("") }
    var customDeviceOSTypeInput by remember { mutableStateOf("RTOS (Lightweight Proprietary OS)") }
    var isCustomWizardExpanded by remember { mutableStateOf(false) }

    // Selected sample file state for import
    val sampleFiles = listOf(
        SampleHealthFile(
            name = "Blood_Pressure_Log_May.csv",
            type = "CSV",
            description = "Systolic/diastolic blood pressure list & resting pulse rates from digital BP monitor.",
            content = "Date,Systolic BP,Diastolic BP,Pulse Rate,Notes\n2026-06-08 08:30,120,80,68,Morning vitals\n2026-06-09 08:35,122,81,69,Slightly higher\n2026-06-10 20:15,118,78,71,Evening relaxation state"
        ),
        SampleHealthFile(
            name = "SmartWatch_Activity_Export.csv",
            type = "CSV",
            description = "Wearable sensor output containing active step goals and logged wellness activities.",
            content = "LogDate,TotalSteps,WaterIntakeMl,YogaSessions\n2026-06-10,8250,1500,45m Kundalini Restorative Yoga\n2026-06-11,9400,2000,30m Active Lower-Body Walk"
        ),
        SampleHealthFile(
            name = "Corporate_Financial_Audit.txt",
            type = "TXT",
            description = "Irrelevant non-health report representing spam, tests AI rejection logic.",
            content = "Project Alpha Budget Q3 Allocation Report\n- Infrastructure servers: $15,000\n- Frontend developer wages: $22,000\n- Coffee supplies: $800\nAction items: Deploy React widget before Tuesday."
        )
    )

    var selectedFileIndex by remember { mutableStateOf(0) }
    var customFileText by remember { mutableStateOf("") }
    var useCustomFileText by remember { mutableStateOf(false) }

    if (isPairingLoading) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Bluetooth Pairing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Establishing first-time hardware link to '$pairingDeviceName' over digital RF channels...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showAdminAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminAuthDialog = false
                adminPinEntered = ""
                isPinIncorrect = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Master Admin Login", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Access low-level hardware sync telemetry and data logs. Unauthorized access is restricted.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = adminPinEntered,
                        onValueChange = {
                            adminPinEntered = it
                            isPinIncorrect = false
                        },
                        label = { Text("Enter Master Admin Pin") },
                        placeholder = { Text("Hint: admin123") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isPinIncorrect,
                        supportingText = {
                            if (isPinIncorrect) {
                                Text("Incorrect credentials. Please try again.", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Passcode required for database audit.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPinEntered == "admin123" || adminPinEntered == "1234") {
                            isAdminUnlocked = true
                            showAdminAuthDialog = false
                            Toast.makeText(context, "Welcome, System Master Administrator", Toast.LENGTH_SHORT).show()
                        } else {
                            isPinIncorrect = true
                        }
                    },
                    modifier = Modifier.testTag("admin_pin_submit")
                ) {
                    Text("Unlock Console")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "MY WELLNESS PROFILE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("profile_close")) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    if (isAdminUnlocked) {
                                        isAdminUnlocked = false
                                        Toast.makeText(context, "Master Admin locked", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showAdminAuthDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("lock_admin_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isAdminUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Admin Area",
                                    tint = if (isAdminUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                ) {
                    // Profile Header card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wellness Catalyst",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Unified Health & Wearables Hub",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (devices.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            "${devices.count { it.isConnected }} Active Linked",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Linked Wearable Devices
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Linked Hardware & Smart Devices",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "Establish live telemetry pairing to smartwatches, medical bracelets, biosensors, or smart rings to automatically coordinate workspace wellness statistics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Display list of paired devices
                    if (devices.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DevicesOther,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        "No Wearable Devices Linked",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Link a smartwatch or smart ring below to collect steps, water logs, and workouts automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(devices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("device_card_${device.id}"),
                                shape = RoundedCornerShape(16.dp),
                                border = if (device.isConnected) CardDefaults.outlinedCardBorder() else null
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (device.deviceType) {
                                                        "WATCH" -> Icons.Default.Watch
                                                        "RING" -> Icons.Default.Circle
                                                        else -> Icons.Default.Smartphone
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Column {
                                                Text(
                                                    device.deviceName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (device.isConnected) Color(0xFF4CAF50) else Color.Gray,
                                                        modifier = Modifier.size(8.dp)
                                                    ) {}
                                                    Text(
                                                        if (device.isConnected) "Telemetry Streaming" else "Disconnected",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.unlinkDevice(device.id, device.deviceName)
                                                Toast.makeText(context, "${device.deviceName} unpaired", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("unlink_btn_${device.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Unpair",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Battery Charge: ${device.batteryPercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Connected: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(device.pairedDate))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.syncDeviceData(device.id, device.deviceName, device.deviceType)
                                                Toast.makeText(context, "Sync complete: Telemetry mapped to app!", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("sync_btn_${device.id}")
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text("Sync Data", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Supported smart device pairing triggers
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Pair New Smart Telemetry Device",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Link any smart watch, fitness band, or ring running on any proprietary OS (Nothing OS, Zepp OS, Crest OS, Wear OS, watchOS, or RTOS) to capture on-device steps, water logs, and workouts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                @Composable
                                fun PairOptionItem(title: String, type: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, testId: String) {
                                    val isAlreadyPaired = devices.any { it.deviceName == title }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isAlreadyPaired) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                            .clickable(enabled = !isAlreadyPaired) {
                                                pairingDeviceName = title
                                                pairingDeviceType = type
                                                isPairingLoading = true
                                                coroutineScope.launch {
                                                    delay(1800)
                                                    viewModel.linkDevice(title, type)
                                                    isPairingLoading = false
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Paired successfully to $title",
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                }
                                            }
                                            .padding(12.dp)
                                            .testTag(testId),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                            Column {
                                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (isAlreadyPaired) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Quick presets for major devices (including CMF Nothing, Amazfit, boAt, Apple, Samsung)
                                PairOptionItem(
                                    title = "CMF by Nothing Watch Pro 2",
                                    type = "WATCH",
                                    icon = Icons.Default.Watch,
                                    subtitle = "Custom LightOS Connection (Nothing Ecosystem)",
                                    testId = "pair_cmf_watch"
                                )
                                PairOptionItem(
                                    title = "Amazfit Balance / T-Rex Pro",
                                    type = "WATCH",
                                    icon = Icons.Default.Watch,
                                    subtitle = "Zepp OS Sync (Amazfit Hardware Engine)",
                                    testId = "pair_amazfit_watch"
                                )
                                PairOptionItem(
                                    title = "boAt Wave / Storm Pro series",
                                    type = "WATCH",
                                    icon = Icons.Default.Watch,
                                    subtitle = "boAt Crest OS Link (Highly optimized RTOS)",
                                    testId = "pair_boat_watch"
                                )
                                PairOptionItem(
                                    title = "Oura Health Smart Ring Gen 3",
                                    type = "RING",
                                    icon = Icons.Default.Circle,
                                    subtitle = "Oura proprietary Ring OS BLE telemetry",
                                    testId = "pair_oura_ring"
                                )
                                PairOptionItem(
                                    title = "Standard Wear OS 4 / Ultra",
                                    type = "WATCH",
                                    icon = Icons.Default.Watch,
                                    subtitle = "Google Wearable Sync (Samsung & Pixel watches)",
                                    testId = "pair_wear_os_watch"
                                )
                                PairOptionItem(
                                    title = "Apple Watch Series 9 (watchOS)",
                                    type = "WATCH",
                                    icon = Icons.Default.Watch,
                                    subtitle = "Apple HealthKit & watchOS native interface",
                                    testId = "pair_apple_watch"
                                )
                                PairOptionItem(
                                    title = "Unified Health Connect System",
                                    type = "HEALTH_APP",
                                    icon = Icons.Default.FavoriteBorder,
                                    subtitle = "Samsung Health & Native Android APIs",
                                    testId = "pair_samsung_health"
                                )

                                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                // Dynamic Flexible form for any other Custom Smart Watch and OS brand
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                                        .clickable { isCustomWizardExpanded = !isCustomWizardExpanded }
                                        .padding(12.dp)
                                        .testTag("expand_custom_pairing_btn"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                "Pair Custom Brand smartwatch / OS",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                "Connect any other smart watch running on any custom OS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isCustomWizardExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle Section",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                if (isCustomWizardExpanded) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            "Enter watch hardware and firmware parameters directly to authenticate:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        OutlinedTextField(
                                            value = customDeviceNameInput,
                                            onValueChange = { customDeviceNameInput = it },
                                            label = { Text("Smartwatch Model Name") },
                                            placeholder = { Text("e.g. CMF Watch Pro 2, Amazfit Pop, boAt Storm, Pebble") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("custom_device_name_field")
                                        )

                                        Text(
                                            "Select Device Operating System:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val listOfOSOptions = listOf(
                                            "CMF OS / Nothing WatchOS",
                                            "Zepp OS (Amazfit)",
                                            "boAt Crest OS",
                                            "Wear OS By Google",
                                            "RTOS (Real-Time LightOS)",
                                            "Fitbit/Garmin OS",
                                            "Custom Proprietary"
                                        )

                                        // Horizontal scroll or wrap list of OS chips
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Split into two rows or show a clean flow column of chips
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOfOSOptions.take(3).forEach { os ->
                                                        val isSelected = customDeviceOSTypeInput == os
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                                .clickable { customDeviceOSTypeInput = os }
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = os.substringBefore(" (").substringBefore("/"),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOfOSOptions.drop(3).take(4).forEach { os ->
                                                        val isSelected = customDeviceOSTypeInput == os
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                                .clickable { customDeviceOSTypeInput = os }
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = os.substringBefore(" (").substringBefore("/"),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (customDeviceNameInput.isBlank()) {
                                                    Toast.makeText(context, "Please enter your smartwatch model brand", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val finalTitle = customDeviceNameInput.trim()
                                                    val osInfo = customDeviceOSTypeInput
                                                    pairingDeviceName = "$finalTitle ($osInfo)"
                                                    pairingDeviceType = "WATCH"
                                                    isPairingLoading = true
                                                    coroutineScope.launch {
                                                        delay(1800)
                                                        viewModel.linkDevice(pairingDeviceName, "WATCH")
                                                        isPairingLoading = false
                                                        Toast.makeText(context, "Successfully paired custom watch via universal link!", Toast.LENGTH_LONG).show()
                                                        
                                                        // Reset inputs and close custom section
                                                        customDeviceNameInput = ""
                                                        isCustomWizardExpanded = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .testTag("submit_custom_pairing_btn")
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                            Text("Pair via Universal Link", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: AI Clinical Document Import (Discreet placement as requested)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Medical & Diagnostic Log Import",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "Securely import clinical summaries, ECG logs, or blood pressure monitor readings in PDF, Excel, or CSV files. The on-device health AI screens and parses verified health records before population.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "Select Document Export source to validation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sampleFiles.forEachIndexed { idx, sFile ->
                                        val isSel = selectedFileIndex == idx && !useCustomFileText
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    selectedFileIndex = idx
                                                    useCustomFileText = false
                                                    viewModel.resetImportState()
                                                }
                                                .padding(8.dp)
                                                .testTag("sample_doc_tab_$idx"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                sFile.name.substringBefore("."),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (!useCustomFileText) {
                                    val currentFile = sampleFiles[selectedFileIndex]
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                currentFile.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                currentFile.type,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            currentFile.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Raw Content preview:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                currentFile.content,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .clickable {
                                            useCustomFileText = !useCustomFileText
                                            viewModel.resetImportState()
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = useCustomFileText,
                                        onCheckedChange = {
                                            useCustomFileText = it
                                            viewModel.resetImportState()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Upload Custom pasted text content", style = MaterialTheme.typography.bodySmall)
                                }

                                if (useCustomFileText) {
                                    OutlinedTextField(
                                        value = customFileText,
                                        onValueChange = { customFileText = it },
                                        label = { Text("Paste custom file logs / medical summary text here") },
                                        placeholder = { Text("Example: My blood pressure log: 122/80 on June 10 2026") },
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("custom_doc_input")
                                    )
                                }

                                // State showing import progress
                                when (importState) {
                                    is ImportState.Idle -> {}
                                    is ImportState.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                Text("Gemini Clinical AI compiling verification logs...", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    is ImportState.Success -> {
                                        val succ = importState as ImportState.Success
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                                Column {
                                                    Text("AI Validation SUCCESS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                                    Text("Identified: ${succ.summary}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                                    Text("Successfully extracted and populated ${succ.logsCount} standard metric log items into your local SQLite wellness profile.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                                }
                                            }
                                        }
                                    }
                                    is ImportState.Error -> {
                                        val err = importState as ImportState.Error
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFEBEE))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFD32F2F))
                                                Column {
                                                    Text("AI Validation REJECTED", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                                                    Text(err.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val fName = if (useCustomFileText) "custom_paste_log.txt" else sampleFiles[selectedFileIndex].name
                                        val fType = if (useCustomFileText) "TXT" else sampleFiles[selectedFileIndex].type
                                        val fContent = if (useCustomFileText) customFileText else sampleFiles[selectedFileIndex].content
                                        
                                        if (fContent.isBlank()) {
                                            Toast.makeText(context, "Document content cannot be empty", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.importHealthDocument(fName, fType, fContent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("scan_doc_ai_btn")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Analyze & Import Document with AI")
                                }
                            }
                        }
                    }

                    // Section 3: Hidden Admin Logs (Only visible to master admin)
                    if (isAdminUnlocked) {
                        item {
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), thickness = 2.dp)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Master Admin Logs [SYSTEM SCHEMA]",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Low-level Bluetooth, hardware sync registries, and raw clinical database payload outputs.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.clearAllDeviceLogs()
                                        Toast.makeText(context, "Telemetry log traces erased", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("admin_clear_logs")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Wipe Log Schema", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (adminLogs.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No hardware telemetry logs synced yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(adminLogs) { log ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceDim),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_log_card")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = when {
                                                        log.status == "REJECTED_BY_AI" -> Color(0xFFFFEBEE)
                                                        log.dataType == "CONNECTION" -> Color(0xFFE8F5E9)
                                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                                    }
                                                ) {
                                                    Text(
                                                        log.dataType,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = when {
                                                            log.status == "REJECTED_BY_AI" -> Color(0xFFC62828)
                                                            log.dataType == "CONNECTION" -> Color(0xFF2E7D32)
                                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    log.deviceName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                log.dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            log.value,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "ID: ${log.deviceId} • Date Epoch: ${log.timestamp}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Status: ${log.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.status == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFC62828)
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
    }
}

data class SampleHealthFile(
    val name: String,
    val type: String,
    val description: String,
    val content: String
)
