package com.jnkim.poschedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.domain.model.NotificationCandidate
import com.jnkim.poschedule.domain.model.NotificationClass
import com.jnkim.poschedule.notifications.NotificationHelper
import com.jnkim.poschedule.utils.DeviceCompatibilityHelper
import com.jnkim.poschedule.workers.DailyPlanWorker
import com.jnkim.poschedule.workers.NotificationWorker
import java.time.Instant
import java.util.UUID

/**
 * DebugScreen - Hidden developer screen for testing notifications.
 *
 * Features:
 * - Send test notifications manually
 * - Test different notification classes (CORE/UPDATE/RECOVERY)
 * - Trigger workers manually
 * - Test notification compatibility (Samsung/Xiaomi)
 *
 * Access: Triple-tap on Settings screen title (hidden easter egg)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    notificationHelper: NotificationHelper,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    var testCounter by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Test Notifications
            SectionHeader("Test Notifications")
            Text(
                text = "Manually send test notifications to verify functionality on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            DebugButton(
                icon = Icons.Default.Notifications,
                label = "Send CORE Notification",
                description = "High priority (medications, meals)"
            ) {
                testCounter++
                notificationHelper.showNotification(
                    candidate = NotificationCandidate(
                        id = "debug_core_${UUID.randomUUID()}",
                        clazz = NotificationClass.CORE,
                        urgency = 0.9,
                        importance = 0.9,
                        actionability = 1.0,
                        userCost = 0.2,
                        deadline = Instant.now().plusSeconds(3600)
                    ),
                    title = "🧪 Test CORE Notification #$testCounter",
                    body = "This is a high-priority test. Tap Done/Skip/Snooze to test actions."
                )
            }

            DebugButton(
                icon = Icons.Default.NotificationsActive,
                label = "Send UPDATE Notification",
                description = "Default priority (schedule updates)"
            ) {
                testCounter++
                notificationHelper.showNotification(
                    candidate = NotificationCandidate(
                        id = "debug_update_${UUID.randomUUID()}",
                        clazz = NotificationClass.UPDATE,
                        urgency = 0.5,
                        importance = 0.6,
                        actionability = 1.0,
                        userCost = 0.3,
                        deadline = Instant.now().plusSeconds(3600)
                    ),
                    title = "🔔 Test UPDATE Notification #$testCounter",
                    body = "Your plan has been adjusted. This is a test notification."
                )
            }

            DebugButton(
                icon = Icons.Default.Notifications,
                label = "Send RECOVERY Notification",
                description = "Low priority (gentle nudges)"
            ) {
                testCounter++
                notificationHelper.showNotification(
                    candidate = NotificationCandidate(
                        id = "debug_recovery_${UUID.randomUUID()}",
                        clazz = NotificationClass.RECOVERY,
                        urgency = 0.3,
                        importance = 0.4,
                        actionability = 0.8,
                        userCost = 0.5,
                        deadline = Instant.now().plusSeconds(3600)
                    ),
                    title = "💚 Test RECOVERY Notification #$testCounter",
                    body = "하나씩 차근차근 해봐요. Recovery mode test."
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Section 2: Worker Testing
            SectionHeader("Worker Testing")
            Text(
                text = "Manually trigger background workers for testing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            DebugButton(
                icon = Icons.Default.Schedule,
                label = "Run DailyPlanWorker Now",
                description = "Expands recurring plans and schedules notifications"
            ) {
                val workRequest = OneTimeWorkRequestBuilder<DailyPlanWorker>()
                    .addTag("debug_trigger")
                    .build()
                workManager.enqueue(workRequest)
            }

            DebugButton(
                icon = Icons.Default.Notifications,
                label = "Run NotificationWorker Now",
                description = "Checks pending items and sends notifications"
            ) {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .addTag("debug_trigger")
                    .build()
                workManager.enqueue(workRequest)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Section 3: Device Compatibility Info
            SectionHeader("Device Compatibility")

            DeviceInfoCard(
                manufacturer = android.os.Build.MANUFACTURER,
                model = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE,
                sdkInt = android.os.Build.VERSION.SDK_INT,
                isBatteryOptimized = !DeviceCompatibilityHelper.isBatteryOptimizationDisabled(context),
                isAggressiveDevice = DeviceCompatibilityHelper.isAggressiveBatteryDevice()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Device Setup Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DeviceCompatibilityHelper.getBatteryOptimizationGuidance(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun DebugButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    manufacturer: String,
    model: String,
    androidVersion: String,
    sdkInt: Int,
    isBatteryOptimized: Boolean,
    isAggressiveDevice: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manufacturer: $manufacturer",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Model: $model",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Android: $androidVersion (SDK $sdkInt)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Battery optimization status
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery Optimization:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isBatteryOptimized) "⚠️ Enabled (may block notifications)" else "✓ Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isBatteryOptimized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Special warnings for known problematic manufacturers
            if (isAggressiveDevice) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ ${if (DeviceCompatibilityHelper.isXiaomiDevice()) "Xiaomi" else "Samsung"} device detected: Additional settings required for reliable notifications!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
