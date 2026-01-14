package com.jnkim.poschedule.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Glassmorphism Base Colors
val GlassWhite = Color(0xA6FFFFFF) // 65% White
val GlassDark = Color(0x9912141A)  // 60% Dark

// Mode Accent Colors
val ModeNormal = Color(0xFFB8B5FF)   // Lavender
val ModeRecovery = Color(0xFF9ECFFF) // Sky Blue
val ModeLowMood = Color(0xFFFFB7A5)  // Peach
val ModeBusy = Color(0xFFF6D365)     // Soft Amber

// Backgrounds
val BackgroundLight = Color(0xFFF5F7FA)
val BackgroundDark = Color(0xFF0E1116)

// === TIME-ADAPTIVE THEME COLORS ===

// DAWN (05:00-07:59) - Soft purples and pinks, like sunrise
object DawnColors {
    val backgroundLight = Color(0xFFFDF4F8)  // Soft pink-white
    val backgroundDark = Color(0xFF1A0F15)   // Deep purple-black
    val surfaceLight = Color(0xFFFEF0F5)
    val surfaceDark = Color(0xFF251520)
    val accentLight = Color(0xFFE8A4C8)      // Soft rose
    val accentDark = Color(0xFFD896C2)
}

// MORNING (08:00-11:59) - Bright, clear sky blues
object MorningColors {
    val backgroundLight = Color(0xFFF8FBFF)  // Very light sky blue
    val backgroundDark = Color(0xFF0D1218)   // Dark blue-black
    val surfaceLight = Color(0xFFF0F6FF)
    val surfaceDark = Color(0xFF161D26)
    val accentLight = Color(0xFFA4C8E8)      // Sky blue
    val accentDark = Color(0xFF8BB4D8)
}

// NOON (12:00-13:59) - Peak brightness, warm whites
object NoonColors {
    val backgroundLight = Color(0xFFFFFDF7)  // Warm white
    val backgroundDark = Color(0xFF0E1116)   // Neutral dark
    val surfaceLight = Color(0xFFFFFBF0)
    val surfaceDark = Color(0xFF1A1D22)
    val accentLight = Color(0xFFFDD87A)      // Bright yellow-gold
    val accentDark = Color(0xFFE6C365)
}

// AFTERNOON (14:00-17:59) - Steady, neutral tones
object AfternoonColors {
    val backgroundLight = Color(0xFFF7F9FB)  // Neutral light
    val backgroundDark = Color(0xFF0F1318)   // Neutral dark
    val surfaceLight = Color(0xFFF2F5F8)
    val surfaceDark = Color(0xFF181C21)
    val accentLight = Color(0xFFB8C8D8)      // Cool gray-blue
    val accentDark = Color(0xFFA4B5C8)
}

// EVENING (18:00-21:59) - Warm oranges and golds, sunset
object EveningColors {
    val backgroundLight = Color(0xFFFFF8F0)  // Warm peach
    val backgroundDark = Color(0xFF181210)   // Warm dark brown
    val surfaceLight = Color(0xFFFFF3E8)
    val surfaceDark = Color(0xFF221D18)
    val accentLight = Color(0xFFFFA86B)      // Sunset orange
    val accentDark = Color(0xFFE89558)
}

// NIGHT (22:00-04:59) - Deep blues and purples, starry night
object NightColors {
    val backgroundLight = Color(0xFFEEF0F8)  // Very dark blue (light mode still readable)
    val backgroundDark = Color(0xFF0A0D14)   // Deep space blue-black
    val surfaceLight = Color(0xFFE8EAF4)
    val surfaceDark = Color(0xFF12151D)
    val accentLight = Color(0xFF8890C8)      // Twilight purple
    val accentDark = Color(0xFF7580B8)
}

// Weather Modifiers (applied as overlays with low alpha)
object WeatherModifiers {
    val cloudyOverlay = Color(0xFF8C9199).copy(alpha = 0.08f)  // Desaturate slightly
    val rainOverlay = Color(0xFF6B7C8C).copy(alpha = 0.12f)    // Cool, darker
    val snowOverlay = Color(0xFFD8E4F0).copy(alpha = 0.10f)    // Lighter, cooler
}
