package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GlassCard with time-adaptive theming support.
 * Now uses MaterialTheme.colorScheme which animates smoothly in time-adaptive mode.
 * The glass effect (alpha transparency) is preserved while colors adapt to the current day phase.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Use surface color from theme (automatically animated in time-adaptive mode)
    // Higher alpha (0.95f) for better visibility in modals and overlays
    val baseColor = colorScheme.surface.copy(alpha = 0.95f)
    val borderColor = colorScheme.outline.copy(alpha = 0.2f)

    Surface(
        modifier = modifier,
        color = baseColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
