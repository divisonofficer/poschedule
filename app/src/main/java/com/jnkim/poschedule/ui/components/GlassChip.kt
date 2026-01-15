package com.jnkim.poschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.isReducedMotionEnabled

/**
 * Glass chip component for selections (day-of-week, tags, etc.).
 *
 * Two variants:
 * - Circle: For day-of-week single-letter chips (40dp)
 * - Rounded: For text labels (pill shape)
 *
 * Design Features:
 * - Selected: accent-tinted glass + scale 1.05
 * - Unselected: translucent outline only
 * - Haptic feedback on selection
 *
 * Theme Integration:
 * - Selected state uses MaterialTheme.colorScheme.primary (animates with day phase)
 * - Unselected uses outline with muted text
 *
 * @param text Chip label
 * @param isSelected Whether chip is selected
 * @param onClick Callback when chip is clicked
 * @param modifier Modifier for the chip
 * @param isCircle Whether to use circle shape (for single letters)
 */
@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCircle: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val isReducedMotion = isReducedMotionEnabled()

    // Animate scale for selection (or snap if reduced motion)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "chipScale"
    )

    // Background color (accent-tinted glass when selected)
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            colorScheme.primary.copy(alpha = 0.25f)
        } else {
            colorScheme.surface.copy(alpha = 0.02f)
        },
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "chipBackground"
    )

    // Border color
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            colorScheme.primary.copy(alpha = 0.6f)
        } else {
            colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderLight)
        },
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "chipBorder"
    )

    // Text color
    val textColor = if (isSelected) {
        colorScheme.primary
    } else {
        colorScheme.onSurface.copy(alpha = DesignTokens.Alpha.muted)
    }

    val shape = if (isCircle) CircleShape else RoundedCornerShape(DesignTokens.Radius.pill)
    val chipModifier = if (isCircle) {
        modifier.size(40.dp)
    } else {
        modifier.height(40.dp)
    }

    Box(
        modifier = chipModifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(
                if (isCircle) PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                else PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * Row of glass chips for day-of-week selection.
 *
 * Displays Monday-Sunday as single-letter circular chips.
 *
 * @param selectedDays Set of selected day numbers (1=Mon, 2=Tue, ..., 7=Sun)
 * @param onDayToggle Callback when day is toggled (day number 1-7)
 * @param modifier Modifier for the row
 * @param dayLabels Optional custom day labels (default: M T W T F S S)
 */
@Composable
fun DayChipRow(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dayLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S")
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNum = index + 1
            GlassChip(
                text = label,
                isSelected = selectedDays.contains(dayNum),
                onClick = { onDayToggle(dayNum) },
                isCircle = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
