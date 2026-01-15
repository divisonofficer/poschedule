package com.jnkim.poschedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.isReducedMotionEnabled

/**
 * Visual style for segmented control.
 *
 * - GLASS: Pill-shaped (999dp radius), subtle glass aesthetic
 * - SOLID: More solid feel (12dp radius), higher contrast for important settings
 */
enum class SegmentedControlStyle {
    GLASS,   // Current default style - pill radius, lower contrast
    SOLID    // Solid style for important settings - sharper corners, higher contrast
}

/**
 * Glass pill segmented control for mutually exclusive options.
 *
 * Design Features:
 * - Pill-shaped container (radius 999dp)
 * - Selected state: accent background + scale 1.02 + glow
 * - Unselected: calm text, no border
 * - Haptic feedback on selection
 *
 * Theme Integration:
 * - Container uses MaterialTheme.colorScheme.surfaceVariant
 * - Selected accent uses MaterialTheme.colorScheme.primary (animates with day phase)
 * - Text colors use onSurface (adapts to light/dark theme)
 *
 * @param options List of option labels
 * @param selectedIndex Currently selected option index (0-based)
 * @param onSelectionChange Callback when selection changes
 * @param modifier Modifier for the container
 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentedControlStyle = SegmentedControlStyle.GLASS
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // Container background (glass surface)
    val containerColor = colorScheme.surfaceVariant.copy(alpha = DesignTokens.Alpha.glassSubtle)

    // Choose radius based on style
    val containerRadius = when (style) {
        SegmentedControlStyle.GLASS -> DesignTokens.Radius.pill
        SegmentedControlStyle.SOLID -> DesignTokens.Radius.sm  // 12.dp
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(containerRadius))
            .background(containerColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedControlOption(
                text = option,
                isSelected = index == selectedIndex,
                onClick = {
                    if (index != selectedIndex) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectionChange(index)
                    }
                },
                modifier = Modifier.weight(1f),
                style = style
            )
        }
    }
}

@Composable
private fun SegmentedControlOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentedControlStyle = SegmentedControlStyle.GLASS
) {
    val colorScheme = MaterialTheme.colorScheme
    val isReducedMotion = isReducedMotionEnabled()

    // Animate scale for selected state (or snap if reduced motion)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) DesignTokens.Animation.scaleSelected else 1.0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "segmentScale"
    )

    // Background color - SOLID style uses slightly lower alpha for better contrast
    val selectedAlpha = when (style) {
        SegmentedControlStyle.GLASS -> 0.2f
        SegmentedControlStyle.SOLID -> 0.15f  // Lower alpha, more solid feel
    }

    val backgroundColor = if (isSelected) {
        colorScheme.primary.copy(alpha = selectedAlpha)
    } else {
        Color.Transparent
    }

    // Text color
    val textColor = if (isSelected) {
        colorScheme.primary
    } else {
        colorScheme.onSurface.copy(alpha = DesignTokens.Alpha.muted)
    }

    // Choose radius based on style
    val optionRadius = when (style) {
        SegmentedControlStyle.GLASS -> DesignTokens.Radius.pill
        SegmentedControlStyle.SOLID -> DesignTokens.Radius.sm  // 12.dp
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(optionRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
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
