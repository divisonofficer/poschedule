package com.jnkim.poschedule.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog

/**
 * IntentSafetyHelper - Validates external URLs and package names before opening.
 *
 * Security Features:
 * - Allowlist-based URL scheme validation
 * - Package name validation for deep links
 * - First-launch confirmation dialog for external apps
 * - Prevents XSS, javascript:, file:, and other malicious schemes
 */
object IntentSafetyHelper {

    private const val TAG = "IntentSafety"
    private const val PREFS_NAME = "intent_safety"
    private const val KEY_HAS_SHOWN_WARNING = "has_shown_external_warning"

    // Allowlisted URL schemes (only safe protocols)
    private val SAFE_SCHEMES = setOf(
        "https", "http",                    // Web
        "geo", "google.navigation",         // Maps
        "mailto",                           // Email
        "tel", "sms",                       // Phone/SMS
        "zoom.us",                          // Zoom
        "msteams",                          // Microsoft Teams
        "meet.google.com"                   // Google Meet
    )

    // Allowlisted Android packages (trusted apps only)
    private val SAFE_PACKAGES = setOf(
        "com.google.android.apps.maps",
        "com.android.chrome",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.google.android.apps.meetings"
    )

    /**
     * Validates if a URL is safe to open.
     *
     * @param url The URL to validate
     * @return The URL if safe, null if unsafe or malformed
     *
     * Security: Blocks javascript:, file:, data:, and other dangerous schemes
     */
    fun validateUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()

            when {
                scheme == null -> {
                    Log.w(TAG, "Blocked URL with no scheme: $url")
                    null
                }
                scheme in SAFE_SCHEMES -> url
                else -> {
                    Log.w(TAG, "Blocked unsafe URL scheme: $scheme in $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL format: $url", e)
            null
        }
    }

    /**
     * Validates if a package name is allowlisted.
     *
     * @param packageName Android package name (e.g., "com.slack")
     * @return true if package is trusted, false otherwise
     */
    fun validatePackage(packageName: String): Boolean {
        return packageName in SAFE_PACKAGES
    }

    /**
     * Shows confirmation dialog before opening external app (first launch only).
     *
     * Uses SharedPreferences to remember user consent.
     * After first confirmation, subsequent opens happen automatically.
     *
     * @param context Android context
     * @param url The URL being opened (shown to user)
     * @param onConfirm Callback executed when user approves
     */
    fun showFirstLaunchConfirmation(
        context: Context,
        url: String,
        onConfirm: () -> Unit
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasShownBefore = prefs.getBoolean(KEY_HAS_SHOWN_WARNING, false)

        if (hasShownBefore) {
            // User already confirmed once, skip dialog
            onConfirm()
            return
        }

        // Show first-launch confirmation
        AlertDialog.Builder(context)
            .setTitle("Open External App")
            .setMessage("This will open an external app or website:\n\n$url\n\nContinue?")
            .setPositiveButton("Open") { _, _ ->
                // Remember user consent
                prefs.edit().putBoolean(KEY_HAS_SHOWN_WARNING, true).apply()
                Log.d(TAG, "User confirmed external app launch for: $url")
                onConfirm()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "User cancelled external app launch for: $url")
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Resets the "has shown warning" flag.
     * Useful for testing or user-requested security reset.
     */
    fun resetWarningFlag(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HAS_SHOWN_WARNING)
            .apply()
        Log.d(TAG, "Reset external app warning flag")
    }
}
