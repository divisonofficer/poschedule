package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.ai.AlternativePlan
import com.jnkim.poschedule.data.ai.NormalizedPlan

/**
 * Review sheet for LLM-normalized plan.
 * Shows parsed plan details with confidence warning if needed.
 * Allows selecting additional alternatives to save together.
 */
@Composable
fun PlanReviewSheet(
    normalizedPlan: NormalizedPlan,
    confidence: Double,
    alternatives: List<AlternativePlan> = emptyList(),
    onConfirm: (List<AlternativePlan>) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track which alternatives are selected (all selected by default)
    var selectedAlternatives by remember { mutableStateOf(alternatives.toSet()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.llm_review_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show confidence warning if low
        if (confidence < 0.85) {
            ConfidenceWarning(confidence)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Display main plan details
        Text(
            text = "Main Task:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        PlanDetailCard(normalizedPlan)

        // Display alternatives if any
        if (alternatives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Additional Tasks (select to add):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            alternatives.forEach { alt ->
                AlternativeCheckbox(
                    alternative = alt,
                    isSelected = selectedAlternatives.contains(alt),
                    onToggle = {
                        selectedAlternatives = if (selectedAlternatives.contains(alt)) {
                            selectedAlternatives - alt
                        } else {
                            selectedAlternatives + alt
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text(stringResource(R.string.action_edit_manually))
                }

                Button(onClick = { onConfirm(selectedAlternatives.toList()) }) {
                    val totalTasks = 1 + selectedAlternatives.size
                    Text(stringResource(R.string.action_confirm_save) + " ($totalTasks)")
                }
            }
        }
    }
}

/**
 * Warning card for low confidence predictions.
 */
@Composable
private fun ConfidenceWarning(
    confidence: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.llm_low_confidence_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Card displaying the parsed plan details.
 */
@Composable
private fun PlanDetailCard(
    plan: NormalizedPlan,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title with emoji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!plan.iconEmoji.isNullOrBlank()) {
                    Text(
                        text = plan.iconEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Plan type and importance
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(plan.planType) }
                )
                if (plan.routineType != null) {
                    AssistChip(
                        onClick = { },
                        label = { Text(plan.routineType) }
                    )
                }
            }

            Divider()

            // Time details
            Text(
                text = "Time: ${formatTimeDetails(plan)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Recurrence details
            Text(
                text = "Repeats: ${formatRecurrence(plan)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Note if present
            if (!plan.note.isNullOrBlank()) {
                Divider()
                Text(
                    text = plan.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Checkbox row for alternative task selection.
 */
@Composable
private fun AlternativeCheckbox(
    alternative: AlternativePlan,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Emoji if available
            if (!alternative.iconEmoji.isNullOrBlank()) {
                Text(
                    text = alternative.iconEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alternative.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = alternative.planType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alternative.routineType != null) {
                        Text(
                            text = "• ${alternative.routineType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats time details for display.
 */
private fun formatTimeDetails(plan: NormalizedPlan): String {
    return when (plan.time.anchor.uppercase()) {
        "WAKE" -> {
            val offset = plan.time.offset ?: 0
            if (offset > 0) {
                "$offset minutes after waking"
            } else if (offset < 0) {
                "${-offset} minutes before waking"
            } else {
                "At wake time"
            }
        }
        "BED" -> {
            val offset = plan.time.offset ?: 0
            if (offset > 0) {
                "$offset minutes after bedtime"
            } else if (offset < 0) {
                "${-offset} minutes before bedtime"
            } else {
                "At bedtime"
            }
        }
        "FIXED" -> {
            val hour = plan.time.fixedHour ?: 9
            val minute = plan.time.fixedMinute ?: 0
            String.format("%02d:%02d", hour, minute)
        }
        else -> "Flexible timing"
    }
}

/**
 * Formats recurrence details for display.
 */
private fun formatRecurrence(plan: NormalizedPlan): String {
    return when (plan.recurrence.kind.uppercase()) {
        "DAILY" -> "Every day"
        "WEEKLY" -> {
            val days = plan.recurrence.weekdays?.joinToString(", ") { dayName(it) } ?: "Weekly"
            "Every $days"
        }
        "WEEKDAYS" -> "Every weekday (Mon-Fri)"
        "MONTHLY" -> {
            val day = plan.recurrence.monthDay ?: 1
            "Monthly on day $day"
        }
        else -> "Does not repeat"
    }
}

/**
 * Converts day number to name.
 * 1=Monday, 7=Sunday
 */
private fun dayName(day: Int): String {
    return when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> "Day $day"
    }
}
