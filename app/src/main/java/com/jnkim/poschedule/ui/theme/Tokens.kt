package com.jnkim.poschedule.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design system tokens for Poschedule 2026 UI.
 *
 * These tokens define the material layers, spacing, and effects
 * that work on top of the time-adaptive color system.
 *
 * CRITICAL: These tokens are ADDITIVE - they do not replace or modify
 * the existing DayPhase/Weather color generation logic.
 *
 * All components should consume MaterialTheme.colorScheme for colors
 * (which animates automatically with time/weather) and use these tokens
 * for sizing, spacing, and effects.
 */
object DesignTokens {

    // === MATERIAL LAYERS ===
    // Following GNOME4 calm + macOS glass depth strategy
    // User requirement: Modals must have ≥ 0.85 opacity for readability

    object Layer {
        // Layer 0 — Background: gradient + subtle noise + time-adaptive palette
        // (Handled by existing GlassBackground component)

        // Layer 1 — Glass Surface: cards, panels, sheets
        val surfaceRadius = 24.dp
        val surfaceAlpha = 0.95f  // Well above 0.85 minimum
        val surfaceBorderAlpha = 0.2f
        val surfaceElevation = 2.dp

        // Layer 2 — Elevated Glass: floating controls (FAB/camera)
        val elevatedRadius = 28.dp
        val elevatedAlpha = 0.97f  // High opacity for floating controls
        val elevatedBorderAlpha = 0.25f
        val elevatedElevation = 6.dp

        // Layer 3 — Focused Surface: focused input / selected segments
        val focusedRadius = 20.dp
        val focusedAlpha = 0.98f  // Highest opacity for focused elements
        val focusedBorderAlpha = 0.3f
        val focusedElevation = 4.dp

        // Layer 4 — Modals: bottom sheets and dialogs
        val modalRadius = 32.dp
        val modalAlpha = 0.92f  // ≥ 0.85 as per user requirement (0.92 = 92%)
        val modalBorderAlpha = 0.2f
        val modalElevation = 8.dp
    }

    // === RADII ===
    object Radius {
        val xs = 8.dp      // Small chips, badges
        val sm = 12.dp     // Buttons, small cards
        val md = 20.dp     // Medium cards, focused inputs
        val lg = 24.dp     // Large cards (current default)
        val xl = 28.dp     // Bottom sheets, elevated cards
        val xxl = 32.dp    // Large modals, dialogs
        val pill = 999.dp  // Segmented controls, toggles, buttons
    }

    // === SPACING ===
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp

        // Semantic spacing (from DESIGN_REFINEMENT_PLAN)
        val pageHorizontal = 20.dp
        val cardInner = 20.dp
        val sectionGap = 16.dp
    }

    // === BLUR ===
    object Blur {
        val none = 0.dp
        val light = 8.dp     // Subtle background blur
        val medium = 16.dp   // Glass card blur
        val strong = 24.dp   // Bottom sheet blur
        val intense = 32.dp  // Modal backdrop blur
    }

    // === ALPHA VALUES ===
    object Alpha {
        // Glass surface alpha (all ≥ 0.85 for modals as per user requirement)
        val glassDefault = 0.95f   // Standard glass (95%)
        val glassSubtle = 0.92f    // Slightly more transparent (92%)
        val glassStrong = 0.97f    // Less transparent (97%)
        val glassModal = 0.90f     // Modals and sheets (90%, above 0.85 minimum)

        // Border alpha
        val borderLight = 0.10f
        val borderDefault = 0.20f
        val borderStrong = 0.30f

        // Specular highlights (for light/dark strokes on glass surfaces)
        // Light mode values
        val specularLight = 0.18f    // Top-left highlight (light mode)
        val specularDark = 0.10f      // Bottom-right shadow (light mode)

        // Dark mode values (softer to avoid harsh contrast)
        val specularLightDark = 0.12f // Top-left highlight (dark mode)
        val specularDarkDark = 0.06f  // Bottom-right shadow (dark mode)

        // State alpha
        val disabled = 0.38f
        val muted = 0.60f
        val focus = 0.12f  // Focus ring overlay

        // Interaction states
        val hoverOverlay = 0.08f
        val pressedOverlay = 0.12f
        val selectedOverlay = 0.16f
    }

    // === TYPOGRAPHY ===
    // Typography scale is defined in Type.kt
    // These tokens are for reference in component documentation
    object TypeScale {
        val h1 = 32.sp        // SemiBold - Page titles, major headings
        val h2 = 20.sp        // SemiBold - Section titles, card headers
        val subtitle = 16.sp  // Medium - Secondary headers
        val body = 16.sp      // Regular - Primary text content
        val caption = 13.sp   // Regular - Secondary text, metadata
        val helper = 12.sp    // Regular - Helper text, hints, micro-copy (more line height)
        val button = 14.sp    // Medium - Button labels
    }

    // === ANIMATION ===
    object Animation {
        // Phase transitions (inherited from existing time-adaptive system)
        const val phaseTransitionMs = 1500  // DO NOT CHANGE - used by animateColorScheme()

        // Component interactions
        const val fastMs = 150      // Quick feedback (press, hover)
        const val normalMs = 300    // Standard transitions (scale, fade)
        const val slowMs = 500      // Deliberate animations (expand, reveal)

        // Scales
        const val scalePressed = 0.96f   // Button press
        const val scaleHover = 1.00f     // Neutral (no scale on hover)
        const val scaleSelected = 1.02f  // Segmented control selection
        const val scaleFocus = 1.00f     // Neutral (no scale on focus)
    }

    // === SHADOWS ===
    // Soft, large radius, low alpha (avoid harsh Material3 defaults)
    object Shadow {
        val cardElevation = 2.dp
        val buttonElevation = 4.dp
        val floatingElevation = 6.dp
        val modalElevation = 8.dp
    }

    // === GLOW EFFECTS ===
    // For inner glow on orbs and selected states
    object Glow {
        val orbRadius = 6.dp        // Timeline orb glow radius
        val focusRadius = 8.dp      // Focus ring glow radius
        val selectedRadius = 10.dp  // Selected item glow radius

        val orbIntensity = 0.3f        // Orb glow alpha
        val focusIntensity = 0.25f     // Focus ring glow alpha
        val selectedIntensity = 0.2f   // Selected item glow alpha
    }

    // === PRIORITY LEVELS ===
    // Visual hierarchy for timeline cards (5-8% color differences)
    object Priority {
        // Background alpha per priority level
        val nowBackgroundAlpha = 0.98f      // +3% vs baseline (urgent items)
        val todayBackgroundAlpha = 0.95f    // Baseline (standard items)
        val relaxedBackgroundAlpha = 0.92f  // -3% vs baseline (future items)

        // Text alpha per priority level
        val nowTextAlpha = 1.0f       // Full opacity for urgent
        val todayTextAlpha = 1.0f     // Full opacity for today
        val relaxedTextAlpha = 0.5f   // Reduced opacity for relaxed

        // Accent color modulation
        val accentBoost = 0.08f        // +8% alpha for NOW level accents
        val accentDesaturate = 0.15f   // -15% saturation for RELAXED level

        // Time window for "NOW" priority (±30 minutes)
        const val nowWindowMinutes = 30
    }

    // === STATUS CARD EFFECTS ===
    // Recovery mode card enhancements for OS-like feel
    object Status {
        val recoveryGradientAlpha = 0.08f    // Subtle vertical gradient intensity
        val recoveryIconGlowRadius = 4.dp     // Glow radius for mode icon
        val recoveryIconGlowIntensity = 0.15f // Glow alpha for mode icon
    }
}

/**
 * Check if reduced motion is enabled in system accessibility settings.
 *
 * When reduced motion is enabled:
 * - Use snap() instead of tween() for animations
 * - Disable decorative animations (but keep functional ones)
 * - Reduce animation durations
 *
 * Usage:
 * ```
 * val isReducedMotion = isReducedMotionEnabled()
 * val animationSpec = if (isReducedMotion) snap() else tween(300)
 * ```
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (e: Exception) {
        false
    }
}
