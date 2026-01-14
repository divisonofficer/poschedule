package com.jnkim.poschedule.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jnkim.poschedule.domain.model.DayPhase
import com.jnkim.poschedule.domain.model.WeatherState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Keep existing color schemes for backward compatibility
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Composable that provides time-adaptive theming with smooth color transitions.
 *
 * Theme Modes:
 * - "SYSTEM": Uses system dark mode setting (Material You on Android 12+)
 * - "LIGHT": Forces light theme
 * - "DARK": Forces dark theme
 * - "TIME_ADAPTIVE": Changes colors based on time of day and weather
 *
 * CRITICAL TIMEZONE BEHAVIOR:
 * - Uses DayPhase.fromCurrentTime() which reads LocalTime.now()
 * - Always uses user's LOCAL timezone (e.g., Korea UTC+9)
 * - Auto-updates every 60 seconds to detect phase transitions
 *
 * @param themeMode The theme mode ("SYSTEM", "LIGHT", "DARK", "TIME_ADAPTIVE")
 * @param weatherEffectsEnabled Whether to apply weather overlays
 * @param manualWeatherState The current weather state as a string
 * @param content The composable content to theme
 */
@Composable
fun PoscheduleTheme(
    themeMode: String = "TIME_ADAPTIVE",
    weatherEffectsEnabled: Boolean = true,
    manualWeatherState: String = "CLEAR",
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    // Determine if we should use dark theme
    val useDarkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        "TIME_ADAPTIVE" -> {
            // Time-adaptive mode uses system preference for light/dark
            // but changes colors within that mode based on time
            systemDarkTheme
        }
        else -> systemDarkTheme // "SYSTEM" or unknown
    }

    // Get current day phase and weather state
    var currentPhase by remember { mutableStateOf(DayPhase.fromCurrentTime()) }
    val currentWeather = remember(manualWeatherState) {
        try {
            WeatherState.valueOf(manualWeatherState)
        } catch (e: IllegalArgumentException) {
            WeatherState.CLEAR
        }
    }

    // Auto-update phase every 60 seconds when in TIME_ADAPTIVE mode
    LaunchedEffect(themeMode) {
        if (themeMode == "TIME_ADAPTIVE") {
            while (isActive) {
                delay(60_000) // Check every minute
                val newPhase = DayPhase.fromCurrentTime()
                if (newPhase != currentPhase) {
                    currentPhase = newPhase
                }
            }
        }
    }

    // Select color scheme based on mode
    val targetColorScheme = when (themeMode) {
        "TIME_ADAPTIVE" -> {
            val weather = if (weatherEffectsEnabled) currentWeather else WeatherState.CLEAR
            TimeAdaptiveColorScheme.getColorScheme(currentPhase, weather, useDarkTheme)
        }
        else -> {
            // SYSTEM, LIGHT, DARK modes
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                useDarkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
    }

    // Animate color transitions smoothly (1.5 second duration)
    val animatedColorScheme = animateColorScheme(targetColorScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Animates all colors in a ColorScheme with smooth transitions.
 * Transition duration: 1500ms (1.5 seconds) for subtle, non-distracting changes.
 *
 * This prevents jarring color jumps when transitioning between day phases
 * or when theme mode changes.
 *
 * @param targetScheme The target color scheme to animate to
 * @return An animated color scheme with all colors transitioning smoothly
 */
@Composable
private fun animateColorScheme(targetScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 1500)

    val primary by animateColorAsState(targetScheme.primary, animationSpec, label = "primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, animationSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(targetScheme.primaryContainer, animationSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(targetScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer")

    val secondary by animateColorAsState(targetScheme.secondary, animationSpec, label = "secondary")
    val onSecondary by animateColorAsState(targetScheme.onSecondary, animationSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(targetScheme.secondaryContainer, animationSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(targetScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer")

    val tertiary by animateColorAsState(targetScheme.tertiary, animationSpec, label = "tertiary")
    val onTertiary by animateColorAsState(targetScheme.onTertiary, animationSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(targetScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(targetScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer")

    val background by animateColorAsState(targetScheme.background, animationSpec, label = "background")
    val onBackground by animateColorAsState(targetScheme.onBackground, animationSpec, label = "onBackground")

    val surface by animateColorAsState(targetScheme.surface, animationSpec, label = "surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, animationSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(targetScheme.surfaceVariant, animationSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant")

    val error by animateColorAsState(targetScheme.error, animationSpec, label = "error")
    val onError by animateColorAsState(targetScheme.onError, animationSpec, label = "onError")
    val errorContainer by animateColorAsState(targetScheme.errorContainer, animationSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(targetScheme.onErrorContainer, animationSpec, label = "onErrorContainer")

    val outline by animateColorAsState(targetScheme.outline, animationSpec, label = "outline")
    val outlineVariant by animateColorAsState(targetScheme.outlineVariant, animationSpec, label = "outlineVariant")
    val scrim by animateColorAsState(targetScheme.scrim, animationSpec, label = "scrim")

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = targetScheme.inversePrimary,

        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,

        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,

        background = background,
        onBackground = onBackground,

        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = targetScheme.surfaceTint,
        inverseSurface = targetScheme.inverseSurface,
        inverseOnSurface = targetScheme.inverseOnSurface,

        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,

        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim
    )
}
