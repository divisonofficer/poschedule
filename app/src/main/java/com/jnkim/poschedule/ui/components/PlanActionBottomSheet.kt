package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.local.entity.PlanItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanActionBottomSheet(
    item: PlanItemEntity,
    onDismiss: () -> Unit,
    onSnooze: (String) -> Unit,
    onSkip: (String) -> Unit,
    onDelete: (String) -> Unit,
    onStopSeries: (String) -> Unit
) {
    var showStopConfirmation by remember { mutableStateOf(false) }

    // Confirmation dialog for stopping recurring series
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text("Stop this routine?") },
            text = {
                Text(
                    "This will stop all future occurrences of \"${item.title}\". " +
                    "Past completions will be preserved.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirmation = false
                        onStopSeries(item.seriesId!!)
                        onDismiss()
                    }
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Adjusting your plan helps protect your mental energy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ActionRow(
                icon = Icons.Default.NotificationsPaused,
                label = "Snooze 15m",
                onClick = { onSnooze(item.id); onDismiss() }
            )
            
            ActionRow(
                icon = Icons.Default.SkipNext,
                label = stringResource(R.string.action_skip),
                onClick = { onSkip(item.id); onDismiss() }
            )

            if (item.seriesId != null) {
                ActionRow(
                    icon = Icons.Default.RemoveCircleOutline,
                    label = "Stop repeating this routine",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showStopConfirmation = true }
                )
            } else {
                ActionRow(
                    icon = Icons.Default.Delete,
                    label = "Remove task",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onDelete(item.id); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}
