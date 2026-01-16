package com.jnkim.poschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.data.local.entity.MeetingType
import java.util.UUID

/**
 * State object for rich plan data input
 */
data class RichDataState(
    val notes: String = "",
    val locationText: String = "",
    val mapQuery: String = "",
    val meetingUrl: String = "",
    val meetingType: MeetingType? = null,
    val contacts: List<ContactInputState> = emptyList(),
    val tags: String = "",
    val colorTag: String? = null
) {
    fun isEmpty(): Boolean =
        notes.isBlank() && locationText.isBlank() && meetingUrl.isBlank() &&
        contacts.isEmpty() && tags.isBlank() && colorTag == null
}

/**
 * State object for a single contact input
 */
data class ContactInputState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = ""
)

/**
 * Reusable rich data input widget with 5 sections:
 * 1. Notes
 * 2. Location
 * 3. Meeting URL
 * 4. Contacts (dynamic list)
 * 5. Tags & Color
 *
 * @param state Current rich data state
 * @param onStateChange Callback when state changes
 * @param onLoadContactsRequest Callback to load device contacts
 * @param hasContactsPermission Whether READ_CONTACTS permission is granted
 */
@Composable
fun RichDataInputWidget(
    state: RichDataState,
    onStateChange: (RichDataState) -> Unit,
    onLoadContactsRequest: () -> Unit,
    hasContactsPermission: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Notes Section
        NotesInputSection(
            notes = state.notes,
            onNotesChange = { onStateChange(state.copy(notes = it)) }
        )

        // 2. Location Section
        LocationInputSection(
            locationText = state.locationText,
            mapQuery = state.mapQuery,
            onLocationChange = { text, query ->
                onStateChange(state.copy(locationText = text, mapQuery = query))
            }
        )

        // 3. Meeting URL Section
        MeetingUrlInputSection(
            meetingUrl = state.meetingUrl,
            detectedType = state.meetingType,
            onUrlChange = { url ->
                val type = detectMeetingType(url)
                onStateChange(state.copy(meetingUrl = url, meetingType = type))
            }
        )

        // 4. Contacts Section (Dynamic List)
        ContactsInputSection(
            contacts = state.contacts,
            onContactsChange = { onStateChange(state.copy(contacts = it)) },
            onLoadContactsRequest = onLoadContactsRequest,
            hasContactsPermission = hasContactsPermission
        )

        // 5. Tags/Color Section
        TagsInputSection(
            tags = state.tags,
            colorTag = state.colorTag,
            onTagsChange = { onStateChange(state.copy(tags = it)) },
            onColorChange = { onStateChange(state.copy(colorTag = it)) }
        )
    }
}

/**
 * Notes input section - multi-line text field
 */
@Composable
private fun NotesInputSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Notes", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 5000) onNotesChange(it) },
                placeholder = { Text("Add notes or details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 5,
                supportingText = {
                    Text(
                        "${notes.length} / 5000",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
    }
}

/**
 * Location input section - location text + optional map query
 */
@Composable
private fun LocationInputSection(
    locationText: String,
    mapQuery: String,
    onLocationChange: (String, String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Location", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = locationText,
                onValueChange = { onLocationChange(it, mapQuery) },
                label = { Text("Location Name") },
                placeholder = { Text("e.g., Conference Room A") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = mapQuery,
                onValueChange = { onLocationChange(locationText, it) },
                label = { Text("Map Query (Optional)") },
                placeholder = { Text("e.g., geo:37.5665,126.9780 or address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * Meeting URL input section with auto-detection of meeting type
 */
@Composable
private fun MeetingUrlInputSection(
    meetingUrl: String,
    detectedType: MeetingType?,
    onUrlChange: (String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Meeting URL", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = meetingUrl,
                onValueChange = onUrlChange,
                label = { Text("Meeting Link") },
                placeholder = { Text("https://zoom.us/j/...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true
            )

            // Show detected meeting type
            AnimatedVisibility(visible = detectedType != null && meetingUrl.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val (icon, label) = when (detectedType) {
                        MeetingType.ZOOM -> "🎥" to "Zoom Meeting"
                        MeetingType.TEAMS -> "👥" to "Microsoft Teams"
                        MeetingType.GOOGLE_MEET -> "📞" to "Google Meet"
                        MeetingType.WEBEX -> "💼" to "Webex"
                        MeetingType.OTHER -> "🔗" to "Other Meeting"
                        null -> "" to ""
                    }
                    if (detectedType != null) {
                        Text(icon, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contacts input section with dynamic add/remove list
 */
@Composable
private fun ContactsInputSection(
    contacts: List<ContactInputState>,
    onContactsChange: (List<ContactInputState>) -> Unit,
    onLoadContactsRequest: () -> Unit,
    hasContactsPermission: Boolean
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Contacts", style = MaterialTheme.typography.titleMedium)
                }

                // Load from device contacts button
                if (hasContactsPermission) {
                    IconButton(onClick = onLoadContactsRequest) {
                        Icon(Icons.Default.ContactPhone, "Load from contacts")
                    }
                }
            }

            // Dynamic contact list
            contacts.forEachIndexed { index, contact ->
                ContactInputCard(
                    contact = contact,
                    onContactChange = { updated ->
                        val newList = contacts.toMutableList()
                        newList[index] = updated
                        onContactsChange(newList)
                    },
                    onRemove = {
                        onContactsChange(contacts.filterIndexed { i, _ -> i != index })
                    },
                    showRemove = contacts.size > 1
                )
            }

            // Add contact button
            OutlinedButton(
                onClick = {
                    onContactsChange(contacts + ContactInputState())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Contact")
            }
        }
    }
}

/**
 * Single contact input card
 */
@Composable
private fun ContactInputCard(
    contact: ContactInputState,
    onContactChange: (ContactInputState) -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showRemove) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Remove contact", modifier = Modifier.size(18.dp))
                    }
                }
            }

            OutlinedTextField(
                value = contact.name,
                onValueChange = { onContactChange(contact.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contact.email,
                onValueChange = { onContactChange(contact.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = contact.phoneNumber,
                onValueChange = { onContactChange(contact.copy(phoneNumber = it)) },
                label = { Text("Phone / KakaoTalk") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            OutlinedTextField(
                value = contact.role,
                onValueChange = { onContactChange(contact.copy(role = it)) },
                label = { Text("Role (Optional)") },
                placeholder = { Text("e.g., Organizer, Attendee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * Tags and color picker section
 */
@Composable
private fun TagsInputSection(
    tags: String,
    colorTag: String?,
    onTagsChange: (String) -> Unit,
    onColorChange: (String?) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Tags & Color", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { if (it.length <= 500) onTagsChange(it) },
                label = { Text("Tags") },
                placeholder = { Text("work, urgent, meeting") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Comma-separated tags") },
                singleLine = true
            )

            // Color picker (simple chip row)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                ColorChip("#FF5733", colorTag, onColorChange) // Red
                ColorChip("#33C3FF", colorTag, onColorChange) // Blue
                ColorChip("#75FF33", colorTag, onColorChange) // Green
                ColorChip("#FFB833", colorTag, onColorChange) // Orange
                ColorChip("#B833FF", colorTag, onColorChange) // Purple
                if (colorTag != null) {
                    IconButton(
                        onClick = { onColorChange(null) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Close, "Clear color", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Color chip for color selection
 */
@Composable
private fun ColorChip(
    color: String,
    selectedColor: String?,
    onSelect: (String) -> Unit
) {
    Surface(
        onClick = { onSelect(color) },
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color(android.graphics.Color.parseColor(color)),
        border = if (selectedColor == color) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {}
}

/**
 * Detect meeting type from URL
 */
private fun detectMeetingType(url: String): MeetingType? {
    return when {
        url.isBlank() -> null
        url.contains("zoom.us", ignoreCase = true) -> MeetingType.ZOOM
        url.contains("teams.microsoft.com", ignoreCase = true) -> MeetingType.TEAMS
        url.contains("meet.google.com", ignoreCase = true) -> MeetingType.GOOGLE_MEET
        url.contains("webex.com", ignoreCase = true) -> MeetingType.WEBEX
        url.startsWith("http", ignoreCase = true) -> MeetingType.OTHER
        else -> null
    }
}
