package com.jnkim.poschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.jnkim.poschedule.domain.model.DayPhase
import com.jnkim.poschedule.domain.model.WeatherState

/**
 * Generates Material 3 ColorScheme based on current time and weather.
 *
 * Priority: DayPhase > WeatherState > Mode
 * - DayPhase determines base colors (the primary influence)
 * - WeatherState applies subtle overlay (secondary influence)
 * - Mode accents are applied at UI component level (existing pattern)
 *
 * The UI should feel like "the sky outside" at the current time.
 */
object TimeAdaptiveColorScheme {

    /**
     * Generates a Material 3 ColorScheme for the given day phase, weather, and theme mode.
     *
     * @param phase The current day phase (DAWN, MORNING, NOON, AFTERNOON, EVENING, NIGHT)
     * @param weather The current weather state (CLEAR, CLOUDY, RAIN, SNOW)
     * @param darkTheme Whether to use dark theme colors
     * @return A Material 3 ColorScheme with time-adaptive colors
     */
    fun getColorScheme(
        phase: DayPhase,
        weather: WeatherState,
        darkTheme: Boolean
    ): ColorScheme {
        // Step 1: Get base colors for day phase
        val baseColors = getPhaseColors(phase, darkTheme)

        // Step 2: Apply weather modifier
        val background = applyWeatherModifier(baseColors.background, weather)
        val surface = applyWeatherModifier(baseColors.surface, weather)

        // Step 3: Build Material 3 ColorScheme
        return if (darkTheme) {
            darkColorScheme(
                primary = baseColors.accent,
                secondary = PurpleGrey80,
                tertiary = Pink80,
                background = background,
                surface = surface,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onTertiary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = baseColors.accent,
                secondary = PurpleGrey40,
                tertiary = Pink40,
                background = background,
                surface = surface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F)
            )
        }
    }

    /**
     * Internal data class to hold colors for a specific phase.
     */
    private data class PhaseColors(
        val background: Color,
        val surface: Color,
        val accent: Color
    )

    /**
     * Gets the base colors for a specific day phase and theme mode.
     */
    private fun getPhaseColors(phase: DayPhase, darkTheme: Boolean): PhaseColors {
        return when (phase) {
            DayPhase.DAWN -> PhaseColors(
                background = if (darkTheme) DawnColors.backgroundDark else DawnColors.backgroundLight,
                surface = if (darkTheme) DawnColors.surfaceDark else DawnColors.surfaceLight,
                accent = if (darkTheme) DawnColors.accentDark else DawnColors.accentLight
            )
            DayPhase.MORNING -> PhaseColors(
                background = if (darkTheme) MorningColors.backgroundDark else MorningColors.backgroundLight,
                surface = if (darkTheme) MorningColors.surfaceDark else MorningColors.surfaceLight,
                accent = if (darkTheme) MorningColors.accentDark else MorningColors.accentLight
            )
            DayPhase.NOON -> PhaseColors(
                background = if (darkTheme) NoonColors.backgroundDark else NoonColors.backgroundLight,
                surface = if (darkTheme) NoonColors.surfaceDark else NoonColors.surfaceLight,
                accent = if (darkTheme) NoonColors.accentDark else NoonColors.accentLight
            )
            DayPhase.AFTERNOON -> PhaseColors(
                background = if (darkTheme) AfternoonColors.backgroundDark else AfternoonColors.backgroundLight,
                surface = if (darkTheme) AfternoonColors.surfaceDark else AfternoonColors.surfaceLight,
                accent = if (darkTheme) AfternoonColors.accentDark else AfternoonColors.accentLight
            )
            DayPhase.EVENING -> PhaseColors(
                background = if (darkTheme) EveningColors.backgroundDark else EveningColors.backgroundLight,
                surface = if (darkTheme) EveningColors.surfaceDark else EveningColors.surfaceLight,
                accent = if (darkTheme) EveningColors.accentDark else EveningColors.accentLight
            )
            DayPhase.NIGHT -> PhaseColors(
                background = if (darkTheme) NightColors.backgroundDark else NightColors.backgroundLight,
                surface = if (darkTheme) NightColors.surfaceDark else NightColors.surfaceLight,
                accent = if (darkTheme) NightColors.accentDark else NightColors.accentLight
            )
        }
    }

    /**
     * Applies weather modifier overlay to a base color.
     * Weather effects are subtle - low alpha overlays that don't drastically change the base color.
     *
     * @param baseColor The base color from the day phase
     * @param weather The weather state
     * @return The base color with weather overlay applied (or unchanged if CLEAR)
     */
    private fun applyWeatherModifier(baseColor: Color, weather: WeatherState): Color {
        return when (weather) {
            WeatherState.CLEAR -> baseColor
            WeatherState.CLOUDY -> WeatherModifiers.cloudyOverlay.compositeOver(baseColor)
            WeatherState.RAIN -> WeatherModifiers.rainOverlay.compositeOver(baseColor)
            WeatherState.SNOW -> WeatherModifiers.snowOverlay.compositeOver(baseColor)
        }
    }
}
