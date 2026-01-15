package com.jnkim.poschedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.specularHighlight

/**
 * Button style variants.
 */
enum class GlassButtonStyle {
    PRIMARY,   // Filled glass with accent gradient
    SECONDARY, // Outline glass with primary border
    TEXT       // Text only with ripple
}

/**
 * Glass-styled button component with multiple style variants.
 *
 * Design Features:
 * - Three variants: PRIMARY (filled), SECONDARY (outline), TEXT
 * - Press scale animation (0.96)
 * - Haptic feedback on press
 * - Pill shape, consistent sizing (48dp height)
 *
 * Theme Integration:
 * - Primary uses MaterialTheme.colorScheme.primary (animates with day phase)
 * - Text/content uses onPrimary/primary for contrast
 * - Press state scales down for tactile feedback
 *
 * @param text Button label
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param style Button style variant (PRIMARY, SECONDARY, TEXT)
 * @param enabled Whether button is enabled
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate press scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) DesignTokens.Animation.scalePressed else 1.0f,
        animationSpec = tween(durationMillis = DesignTokens.Animation.fastMs),
        label = "buttonScale"
    )

    // Colors based on style
    val (backgroundColor, contentColor, border) = when (style) {
        GlassButtonStyle.PRIMARY -> Triple(
            colorScheme.primary.copy(alpha = if (enabled) 0.9f else DesignTokens.Alpha.disabled),
            colorScheme.onPrimary,
            null
        )
        GlassButtonStyle.SECONDARY -> Triple(
            Color.Transparent,
            colorScheme.primary,
            BorderStroke(
                width = 1.dp,
                color = colorScheme.primary.copy(
                    alpha = if (enabled) 0.6f else DesignTokens.Alpha.disabled
                )
            )
        )
        GlassButtonStyle.TEXT -> Triple(
            Color.Transparent,
            colorScheme.primary,
            null
        )
    }

    Surface(
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .then(
                if (style == GlassButtonStyle.PRIMARY && enabled) {
                    Modifier.specularHighlight(
                        cornerRadius = DesignTokens.Radius.pill,
                        intensity = 0.6f
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(DesignTokens.Radius.pill),
        color = backgroundColor,
        border = border,
        interactionSource = interactionSource,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    contentColor
                } else {
                    contentColor.copy(alpha = DesignTokens.Alpha.disabled)
                }
            )
        }
    }
}

/**
 * Row of buttons with standard spacing.
 * Typically used for Cancel/Save button pairs in dialogs/sheets.
 *
 * @param modifier Modifier for the row
 * @param horizontalArrangement Arrangement of buttons (default: SpaceBetween)
 * @param content Button content
 */
@Composable
fun GlassButtonRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
