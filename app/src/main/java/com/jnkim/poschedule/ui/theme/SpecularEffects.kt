package com.jnkim.poschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specular highlight effects for glass surfaces.
 *
 * Adds subtle top-left light stroke and bottom-right dark stroke
 * to create depth and material feel on glass surfaces.
 *
 * Based on macOS 2026 glass design patterns from DESIGN_REFINEMENT_PLAN.
 *
 * IMPORTANT: These effects are ADDITIVE - they work on top of the
 * time-adaptive color system without modifying base colors.
 */

/**
 * Applies specular highlight strokes to a glass surface.
 *
 * Creates depth through two subtle strokes:
 * - Top-left: Light stroke (alpha 0.10-0.18) for highlight
 * - Bottom-right: Dark stroke (alpha 0.06-0.10) for depth
 *
 * The effect automatically adapts to light/dark theme.
 *
 * @param cornerRadius The corner radius of the surface (for proper stroke positioning)
 * @param intensity Multiplier for alpha values (0.0 to 1.0, default 1.0)
 * @return Modified modifier with specular highlights
 */
fun Modifier.specularHighlight(
    cornerRadius: Dp = DesignTokens.Layer.surfaceRadius,
    intensity: Float = 1.0f
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Calculate alpha values based on theme
    val lightAlpha = if (isDark) {
        DesignTokens.Alpha.specularLightDark * intensity
    } else {
        DesignTokens.Alpha.specularLight * intensity
    }

    val darkAlpha = if (isDark) {
        DesignTokens.Alpha.specularDarkDark * intensity
    } else {
        DesignTokens.Alpha.specularDark * intensity
    }

    val lightColor = onSurface.copy(alpha = lightAlpha)
    val darkColor = Color.Black.copy(alpha = darkAlpha)

    this.drawBehind {
        val strokeWidth = 1.dp.toPx()
        val offset = strokeWidth / 2
        val radiusPx = cornerRadius.toPx()

        // Top edge light stroke
        drawLine(
            color = lightColor,
            start = Offset(radiusPx, offset),
            end = Offset(size.width - radiusPx, offset),
            strokeWidth = strokeWidth
        )

        // Left edge light stroke
        drawLine(
            color = lightColor,
            start = Offset(offset, radiusPx),
            end = Offset(offset, size.height - radiusPx),
            strokeWidth = strokeWidth
        )

        // Bottom edge dark stroke
        drawLine(
            color = darkColor,
            start = Offset(radiusPx, size.height - offset),
            end = Offset(size.width - radiusPx, size.height - offset),
            strokeWidth = strokeWidth
        )

        // Right edge dark stroke
        drawLine(
            color = darkColor,
            start = Offset(size.width - offset, radiusPx),
            end = Offset(size.width - offset, size.height - radiusPx),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Applies inner glow effect for orbs and selected states.
 *
 * Creates a soft, radial gradient glow from the center.
 * Color is automatically derived from MaterialTheme.colorScheme.primary
 * (which animates with day phase - dawn pink, morning blue, etc.).
 *
 * @param glowRadius The radius of the glow effect
 * @param intensity Multiplier for alpha value (0.0 to 1.0, default 1.0)
 * @return Modified modifier with inner glow effect
 */
fun Modifier.innerGlow(
    glowRadius: Dp = DesignTokens.Glow.orbRadius,
    intensity: Float = 1.0f
): Modifier = composed {
    val primaryColor = MaterialTheme.colorScheme.primary

    this.drawBehind {
        val glowRadiusPx = glowRadius.toPx()
        val glowAlpha = DesignTokens.Glow.orbIntensity * intensity

        // Draw radial gradient glow from center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha),
                    primaryColor.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = center,
                radius = glowRadiusPx
            ),
            radius = glowRadiusPx,
            center = center
        )
    }
}

/**
 * Applies focus glow effect for focused inputs and selected segments.
 *
 * Similar to innerGlow but designed for rectangular focus rings.
 * Uses a more subtle intensity appropriate for focus states.
 *
 * @param glowRadius The radius of the glow effect
 * @param intensity Multiplier for alpha value (0.0 to 1.0, default 1.0)
 * @return Modified modifier with focus glow effect
 */
fun Modifier.focusGlow(
    glowRadius: Dp = DesignTokens.Glow.focusRadius,
    intensity: Float = 1.0f
): Modifier = composed {
    val primaryColor = MaterialTheme.colorScheme.primary

    this.drawBehind {
        val glowRadiusPx = glowRadius.toPx()
        val glowAlpha = DesignTokens.Glow.focusIntensity * intensity

        // Draw soft glow around entire surface
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha),
                    primaryColor.copy(alpha = glowAlpha * 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = glowRadiusPx
            )
        )
    }
}

/**
 * Applies selection glow effect for selected chips and states.
 *
 * More prominent than focus glow, used for selected items
 * in segmented controls and chip groups.
 *
 * @param glowRadius The radius of the glow effect
 * @param intensity Multiplier for alpha value (0.0 to 1.0, default 1.0)
 * @return Modified modifier with selection glow effect
 */
fun Modifier.selectionGlow(
    glowRadius: Dp = DesignTokens.Glow.selectedRadius,
    intensity: Float = 1.0f
): Modifier = composed {
    val primaryColor = MaterialTheme.colorScheme.primary

    this.drawBehind {
        val glowRadiusPx = glowRadius.toPx()
        val glowAlpha = DesignTokens.Glow.selectedIntensity * intensity

        // Draw selection glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha),
                    primaryColor.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.7f
            )
        )
    }
}
