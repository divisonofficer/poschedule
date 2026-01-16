package com.jnkim.poschedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.ui.components.GlassCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * PlanDetailScreen - Comprehensive detail view for calendar-grade plan items.
 *
 * Features:
 * - Full plan information display with emoji icon and status badge
 * - Quick actions (Done/Snooze/Skip) at top for easy access
 * - Conditional sections (only show if data exists):
 *   - Location with Maps integration
 *   - Join meeting with Zoom/Meet/Teams support
 *   - Contact information with email/copy actions
 *   - Source evidence accordion for transparency
 * - Safe external intent handling via IntentSafetyHelper
 *
 * Phase 1: Rich Plan Detail View
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planItem: PlanItemEntity,
    onDismiss: () -> Unit,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit,
    onOpenMap: (String) -> Unit,
    onJoinMeeting: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Details") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                                onDismiss()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section: Icon, Title, Time, Status
            item {
                HeaderSection(planItem)
            }

            // Quick Actions: Done, Snooze, Skip
            item {
                QuickActionButtons(
                    onDone = onMarkDone,
                    onSnooze = onSnooze,
                    onSkip = onSkip
                )
            }

            // Location Section (conditional)
            if (!planItem.locationText.isNullOrBlank()) {
                item {
                    LocationSection(
                        item = planItem,
                        onOpenMap = onOpenMap
                    )
                }
            }

            // Join/Web Section (conditional)
            if (!planItem.joinUrl.isNullOrBlank() || !planItem.webUrl.isNullOrBlank()) {
                item {
                    JoinSection(
                        item = planItem,
                        onJoinMeeting = onJoinMeeting,
                        onOpenWeb = onOpenWeb
                    )
                }
            }

            // Contact Section (conditional)
            if (!planItem.contactName.isNullOrBlank() || !planItem.contactEmail.isNullOrBlank()) {
                item {
                    ContactSection(
                        item = planItem,
                        onSendEmail = onSendEmail,
                        onCopyText = onCopyText
                    )
                }
            }

            // Source Evidence Accordion (conditional, expandable)
            if (!planItem.sourceSnippet.isNullOrBlank()) {
                item {
                    SourceEvidenceAccordion(planItem.sourceSnippet)
                }
            }
        }
    }
}

/**
 * Header Section: Displays emoji icon, title, time range, and status badge.
 */
@Composable
private fun HeaderSection(item: PlanItemEntity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Emoji + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.iconEmoji ?: "📋",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Time Range (if available)
        if (item.startTimeMillis != null) {
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val start = Instant.ofEpochMilli(item.startTimeMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
            val end = item.endTimeMillis?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter)
            }

            Text(
                text = if (end != null) "$start - $end" else start,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status Badge
        StatusBadge(item.status)
    }
}

/**
 * Status Badge: Visual indicator for plan status (PENDING, DONE, SNOOZED, SKIP).
 */
@Composable
private fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.primary to "Pending"
        "DONE" -> MaterialTheme.colorScheme.tertiary to "Done"
        "SNOOZED" -> MaterialTheme.colorScheme.secondary to "Snoozed"
        "SKIP" -> MaterialTheme.colorScheme.surfaceVariant to "Skipped"
        else -> MaterialTheme.colorScheme.surfaceVariant to status
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * Quick Action Buttons: Done, Snooze, Skip for immediate status changes.
 */
@Composable
private fun QuickActionButtons(
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Done")
        }

        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Snooze")
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Skip")
        }
    }
}

/**
 * Location Section: Shows location text and "Open Maps" button.
 */
@Composable
private fun LocationSection(
    item: PlanItemEntity,
    onOpenMap: (String) -> Unit
) {
    GlassCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Where",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Location Text
            Text(
                text = item.locationText!!,
                style = MaterialTheme.typography.bodyLarge
            )

            // Open Maps Button (only if mapQuery exists)
            if (!item.mapQuery.isNullOrBlank()) {
                Button(
                    onClick = { onOpenMap(item.mapQuery) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Maps")
                }
            }
        }
    }
}

/**
 * Join Section: Meeting URL buttons (Join, View Details).
 */
@Composable
private fun JoinSection(
    item: PlanItemEntity,
    onJoinMeeting: (String) -> Unit,
    onOpenWeb: (String) -> Unit
) {
    GlassCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Join Meeting",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Join Button (primary action)
                if (!item.joinUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onJoinMeeting(item.joinUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Join")
                    }
                }

                // View Details Button (secondary action)
                if (!item.webUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenWeb(item.webUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Details")
                    }
                }
            }
        }
    }
}

/**
 * Contact Section: Shows contact name, email, with Email/Copy actions.
 */
@Composable
private fun ContactSection(
    item: PlanItemEntity,
    onSendEmail: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    GlassCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "People",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Contact Name
            if (!item.contactName.isNullOrBlank()) {
                Text(
                    text = item.contactName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Contact Email + Actions
            if (!item.contactEmail.isNullOrBlank()) {
                Text(
                    text = item.contactEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSendEmail(item.contactEmail) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Email")
                    }

                    OutlinedButton(
                        onClick = { onCopyText(item.contactEmail) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Copy")
                    }
                }
            }
        }
    }
}

/**
 * Source Evidence Accordion: Expandable section showing AI analysis source.
 *
 * Provides transparency by showing what text/image the LLM analyzed
 * to generate this plan item.
 */
@Composable
private fun SourceEvidenceAccordion(snippet: String) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        onClick = { expanded = !expanded }
    ) {
        Column {
            // Header (always visible, clickable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Source,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Source Evidence",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            // Expandable Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "What was analyzed:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
