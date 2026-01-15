package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.data.ai.AlternativePlan
import com.jnkim.poschedule.data.ai.NormalizedPlan
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Review sheet for LLM-normalized plan.
 * Shows parsed plan details with confidence warning if needed.
 * Allows selecting additional alternatives to save together.
 * For one-time events with specificDate, allows editing date and time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanReviewSheet(
    normalizedPlan: NormalizedPlan,
    confidence: Double,
    alternatives: List<AlternativePlan> = emptyList(),
    onConfirm: (NormalizedPlan, List<NormalizedPlan>) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track which alternatives are selected (all selected by default)
    var selectedAlternatives by remember { mutableStateOf(alternatives.toSet()) }

    // Editable plan (for date/time modifications)
    var editablePlan by remember { mutableStateOf(normalizedPlan) }

    // Convert alternatives to editable full plans (inheriting main plan's time/date)
    var editableAlternatives by remember {
        mutableStateOf(
            alternatives.associateWith { alt ->
                NormalizedPlan(
                    title = alt.title,
                    planType = alt.planType,
                    routineType = alt.routineType,
                    iconEmoji = alt.iconEmoji,
                    importance = "MEDIUM",
                    time = normalizedPlan.time,
                    recurrence = normalizedPlan.recurrence,
                    specificDate = normalizedPlan.specificDate,
                    note = null
                )
            }
        )
    }

    // Date/Time picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlternative by remember { mutableStateOf<AlternativePlan?>(null) }

    // Check if this is a one-time event
    val isOneTimeEvent = editablePlan.recurrence.kind.uppercase() == "NONE" &&
                         editablePlan.specificDate != null

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
        PlanDetailCard(
            plan = editablePlan,
            isEditable = isOneTimeEvent,
            onDateClick = { showDatePicker = true },
            onTimeClick = { showTimePicker = true }
        )

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
                AlternativeCard(
                    alternative = alt,
                    editablePlan = editableAlternatives[alt]!!,
                    isSelected = selectedAlternatives.contains(alt),
                    isEditable = isOneTimeEvent,
                    onToggle = {
                        selectedAlternatives = if (selectedAlternatives.contains(alt)) {
                            selectedAlternatives - alt
                        } else {
                            selectedAlternatives + alt
                        }
                    },
                    onDateClick = {
                        editingAlternative = alt
                        showDatePicker = true
                    },
                    onTimeClick = {
                        editingAlternative = alt
                        showTimePicker = true
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

                Button(onClick = {
                    // Pass edited versions of selected alternatives
                    val editedAlts = selectedAlternatives.mapNotNull { alt ->
                        editableAlternatives[alt]
                    }
                    onConfirm(editablePlan, editedAlts)
                }) {
                    val totalTasks = 1 + selectedAlternatives.size
                    Text(stringResource(R.string.action_confirm_save) + " ($totalTasks)")
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val currentPlan = editingAlternative?.let { editableAlternatives[it] } ?: editablePlan
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentPlan.specificDate?.let {
                try {
                    LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                editingAlternative = null
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant
                            .ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val newDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                        if (editingAlternative != null) {
                            // Update alternative
                            editableAlternatives = editableAlternatives.mapValues { (key, plan) ->
                                if (key == editingAlternative) {
                                    plan.copy(specificDate = newDateStr)
                                } else {
                                    plan
                                }
                            }
                        } else {
                            // Update main plan
                            editablePlan = editablePlan.copy(specificDate = newDateStr)
                        }
                    }
                    showDatePicker = false
                    editingAlternative = null
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    editingAlternative = null
                }) {
                    Text("취소")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val currentPlan = editingAlternative?.let { editableAlternatives[it] } ?: editablePlan
        val timePickerState = rememberTimePickerState(
            initialHour = currentPlan.time.fixedHour ?: 9,
            initialMinute = currentPlan.time.fixedMinute ?: 0
        )

        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                editingAlternative = null
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingAlternative != null) {
                        // Update alternative
                        editableAlternatives = editableAlternatives.mapValues { (key, plan) ->
                            if (key == editingAlternative) {
                                plan.copy(
                                    time = plan.time.copy(
                                        fixedHour = timePickerState.hour,
                                        fixedMinute = timePickerState.minute
                                    )
                                )
                            } else {
                                plan
                            }
                        }
                    } else {
                        // Update main plan
                        editablePlan = editablePlan.copy(
                            time = editablePlan.time.copy(
                                fixedHour = timePickerState.hour,
                                fixedMinute = timePickerState.minute
                            )
                        )
                    }
                    showTimePicker = false
                    editingAlternative = null
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    editingAlternative = null
                }) {
                    Text("취소")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
 * For one-time events, shows clickable date/time fields.
 */
@Composable
private fun PlanDetailCard(
    plan: NormalizedPlan,
    isEditable: Boolean = false,
    onDateClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
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

            // Date (for one-time events)
            if (plan.specificDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isEditable) Modifier.clickable(onClick = onDateClick)
                            else Modifier
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = if (isEditable) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = try {
                            val date = LocalDate.parse(plan.specificDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", java.util.Locale.KOREAN))
                        } catch (e: Exception) {
                            plan.specificDate
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEditable) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditable) {
                        Text(
                            text = "(탭하여 수정)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Time details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEditable && plan.time.anchor.uppercase() == "FIXED")
                            Modifier.clickable(onClick = onTimeClick)
                        else Modifier
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Time",
                    tint = if (isEditable && plan.time.anchor.uppercase() == "FIXED")
                           MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "시간: ${formatTimeDetails(plan)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEditable && plan.time.anchor.uppercase() == "FIXED")
                            MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                if (isEditable && plan.time.anchor.uppercase() == "FIXED") {
                    Text(
                        text = "(탭하여 수정)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recurrence details
            Text(
                text = "반복: ${formatRecurrence(plan)}",
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
 * Expandable card for alternative task selection and editing.
 */
@Composable
private fun AlternativeCard(
    alternative: AlternativePlan,
    editablePlan: NormalizedPlan,
    isSelected: Boolean,
    isEditable: Boolean,
    onToggle: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header row with checkbox and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
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

                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "접기" else "펼치기"
                )
            }

            // Expanded details
            if (isExpanded) {
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date (for one-time events)
                    if (editablePlan.specificDate != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isEditable) Modifier.clickable(onClick = onDateClick)
                                    else Modifier
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date",
                                tint = if (isEditable) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = try {
                                    val date = LocalDate.parse(editablePlan.specificDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                    date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", java.util.Locale.KOREAN))
                                } catch (e: Exception) {
                                    editablePlan.specificDate
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isEditable) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            if (isEditable) {
                                Text(
                                    text = "(탭하여 수정)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Time details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isEditable && editablePlan.time.anchor.uppercase() == "FIXED")
                                    Modifier.clickable(onClick = onTimeClick)
                                else Modifier
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = if (isEditable && editablePlan.time.anchor.uppercase() == "FIXED")
                                   MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "시간: ${formatTimeDetails(editablePlan)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEditable && editablePlan.time.anchor.uppercase() == "FIXED")
                                    MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        if (isEditable && editablePlan.time.anchor.uppercase() == "FIXED") {
                            Text(
                                text = "(탭하여 수정)",
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
