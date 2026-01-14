package com.jnkim.poschedule.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * DeviceCompatibilityHelper - Utility for handling OEM-specific notification issues.
 *
 * Problem:
 * - Xiaomi (MIUI/HyperOS): Aggressive battery optimization kills background workers
 * - Samsung: OneUI battery optimization affects notification delivery
 *
 * Solution:
 * - Detect problematic manufacturers
 * - Check battery optimization status
 * - Provide user-facing guidance
 */
object DeviceCompatibilityHelper {

    /**
     * Check if the device is a Xiaomi device (MIUI/HyperOS).
     */
    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer == "xiaomi" || manufacturer == "redmi"
    }

    /**
     * Check if the device is a Samsung device (OneUI).
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    /**
     * Check if the device is known to have aggressive battery optimization.
     */
    fun isAggressiveBatteryDevice(): Boolean {
        return isXiaomiDevice() || isSamsungDevice()
    }

    /**
     * Check if the app is whitelisted from battery optimization.
     *
     * @return true if the app can run in the background without restrictions
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Get user-facing message for enabling notifications on this device.
     *
     * @return Device-specific instructions or generic message
     */
    fun getBatteryOptimizationGuidance(context: Context): String {
        return when {
            isXiaomiDevice() -> """
                Xiaomi (HyperOS/MIUI) detected:

                1. Go to Settings → Apps → Poschedule → Battery saver → No restrictions
                2. Settings → Apps → Poschedule → Autostart → Enable
                3. Settings → Notifications → Poschedule → Allow all channels

                Without these settings, notifications may not appear.
            """.trimIndent()

            isSamsungDevice() -> """
                Samsung device detected:

                1. Settings → Apps → Poschedule → Battery → Unrestricted
                2. Settings → Notifications → Poschedule → Allow all channels
                3. Settings → Battery → Power mode → Standard (avoid Power saving mode)

                Without these settings, notifications may be delayed.
            """.trimIndent()

            else -> """
                For reliable notifications:

                1. Go to Settings → Apps → Poschedule → Battery → Unrestricted
                2. Settings → Notifications → Poschedule → Enable all channels

                This ensures notifications appear on time.
            """.trimIndent()
        }
    }

    /**
     * Get an Intent to open battery optimization settings for this app.
     * Only works on Android M (API 23) and above.
     *
     * @return Intent to battery optimization settings, or null if not available
     */
    fun getBatteryOptimizationSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            null
        }
    }

    /**
     * Get an Intent to request disabling battery optimization for this specific app.
     * Opens a system dialog asking the user to whitelist the app.
     *
     * Note: This requires REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission in manifest,
     * which may be rejected by Google Play for certain app categories.
     * Use with caution and only for apps that genuinely need background execution.
     *
     * @return Intent to request battery optimization exemption
     */
    fun getRequestBatteryOptimizationIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    /**
     * Check if we should show battery optimization prompt to user.
     * Only show on first launch or if device is known to have issues.
     *
     * @return true if we should prompt user to disable battery optimization
     */
    fun shouldRequestBatteryOptimization(context: Context): Boolean {
        return isAggressiveBatteryDevice() && !isBatteryOptimizationDisabled(context)
    }

    /**
     * Get device information string for debugging.
     */
    fun getDeviceInfo(): String {
        return """
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Known Issues: ${if (isAggressiveBatteryDevice()) "Aggressive battery optimization" else "None"}
        """.trimIndent()
    }
}
