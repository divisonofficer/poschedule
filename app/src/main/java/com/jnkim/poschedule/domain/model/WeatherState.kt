package com.jnkim.poschedule.domain.model

/**
 * Represents weather conditions for subtle theme overlay effects.
 *
 * Weather is a hint, not a forecast - effects are minimal and subtle.
 * Weather overlays are applied on top of the base day phase colors.
 *
 * Priority: Time of day > Weather > Mode
 */
enum class WeatherState {
    CLEAR,   // Default - no modification to base colors
    CLOUDY,  // Slightly desaturated colors (gray overlay)
    RAIN,    // Cooler tones, slightly darker (blue-gray overlay)
    SNOW     // Lighter, cooler tones (light blue-white overlay)
}
