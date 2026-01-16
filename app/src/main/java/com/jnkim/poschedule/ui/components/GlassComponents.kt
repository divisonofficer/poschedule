package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.specularHighlight

/**
 * GlassCard with time-adaptive theming support.
 * Now uses MaterialTheme.colorScheme which animates smoothly in time-adaptive mode.
 * The glass effect (alpha transparency) is preserved while colors adapt to the current day phase.
 *
 * Enhanced with specular highlights for depth (top-left light, bottom-right dark strokes).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Use surface color from theme (automatically animated in time-adaptive mode)
    // Uses DesignTokens for consistent alpha values
    val baseColor = colorScheme.surface.copy(alpha = DesignTokens.Alpha.glassDefault)
    val borderColor = colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderDefault)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .specularHighlight(cornerRadius = DesignTokens.Layer.surfaceRadius),
        color = baseColor,
        shape = RoundedCornerShape(DesignTokens.Layer.surfaceRadius),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = DesignTokens.Layer.surfaceElevation
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.cardInner),
            content = content
        )
    }
}

/**
 * GlassBackground with time-adaptive theming support.
 * Now uses MaterialTheme.colorScheme.background which changes with day phase.
 * Mode accent colors (passed as parameters) still work and layer on top.
 */
@Composable
fun GlassBackground(
    accentColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        bgColor,
                        bgColor
                    )
                )
            )
    ) {
        content()
    }
}

/**
 * GlassBottomSheet with standardized styling.
 *
 * Design Features:
 * - Standard radius: 28-32dp (top corners)
 * - Opacity: ≥ 0.90 (meets user requirement of minimum 0.85 for readability)
 * - Glass drag handle with gradient
 * - Consistent blur and elevation
 *
 * Theme Integration:
 * - Uses MaterialTheme.colorScheme.surface (time-adaptive)
 * - Drag handle adapts to onSurface color
 *
 * @param onDismissRequest Callback when sheet is dismissed
 * @param modifier Modifier for the sheet
 * @param content Sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = colorScheme.surface.copy(alpha = DesignTokens.Alpha.glassModal),
        shape = RoundedCornerShape(
            topStart = DesignTokens.Layer.modalRadius,
            topEnd = DesignTokens.Layer.modalRadius
        ),
        tonalElevation = DesignTokens.Layer.modalElevation,
        dragHandle = {
            // Custom glass drag handle with gradient
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(DesignTokens.Radius.xs))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.onSurface.copy(alpha = 0.3f),
                                    colorScheme.onSurface.copy(alpha = 0.1f),
                                    colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
        },
        content = content
    )
}
