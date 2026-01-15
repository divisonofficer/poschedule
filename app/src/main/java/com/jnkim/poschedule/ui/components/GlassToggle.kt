package com.jnkim.poschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.isReducedMotionEnabled
import com.jnkim.poschedule.ui.theme.specularHighlight

/**
 * Glass-styled toggle switch with soft gradient track.
 *
 * Design Features:
 * - Soft gradient track (horizontal gradient)
 * - Glass thumb with specular highlight
 * - Checked state uses primary accent gradient
 * - Smooth 300ms animation
 * - Haptic feedback on toggle
 *
 * Theme Integration:
 * - Track uses MaterialTheme.colorScheme.primary when checked (animates with day phase)
 * - Thumb uses MaterialTheme.colorScheme.surface (adapts to light/dark)
 * - Gradient background adapts to theme
 *
 * @param checked Whether toggle is checked
 * @param onCheckedChange Callback when toggle state changes
 * @param modifier Modifier for the toggle
 * @param enabled Whether toggle is enabled
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val isReducedMotion = isReducedMotionEnabled()

    // Animate thumb position (0.0 = left, 1.0 = right) - or snap if reduced motion
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "thumbPosition"
    )

    // Track color (gradient when checked)
    val trackColorStart by animateColorAsState(
        targetValue = if (checked) {
            colorScheme.primary.copy(alpha = 0.6f)
        } else {
            colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "trackColorStart"
    )

    val trackColorEnd by animateColorAsState(
        targetValue = if (checked) {
            colorScheme.primary.copy(alpha = 0.4f)
        } else {
            colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "trackColorEnd"
    )

    // Thumb color (glass surface with high opacity)
    val thumbColor = colorScheme.surface.copy(
        alpha = if (enabled) 0.98f else DesignTokens.Alpha.disabled
    )

    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 24.dp
    val thumbPadding = 2.dp
    val thumbTravel = trackWidth - thumbSize - (thumbPadding * 2)

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(DesignTokens.Radius.pill))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(trackColorStart, trackColorEnd)
                )
            )
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbTravel * thumbPosition)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .specularHighlight(
                    cornerRadius = thumbSize / 2,
                    intensity = if (enabled) 0.8f else 0.3f
                )
        )
    }
}
