package com.jnkim.poschedule.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.jnkim.poschedule.MainActivity
import com.jnkim.poschedule.data.share.SharePayload
import com.jnkim.poschedule.ui.screens.ImportDraftScreen
import com.jnkim.poschedule.ui.theme.PoscheduleTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin Activity that receives share intents and routes to ImportDraftScreen.
 *
 * Pattern: Single Activity Architecture
 * - This Activity is ephemeral and launches solely for share intent processing
 * - No navigation host needed - just a single screen
 * - After user confirms/cancels, this Activity finishes
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse the incoming intent
        val sharePayload = parseShareIntent(intent)

        if (sharePayload == null) {
            // Invalid share data - just close
            finish()
            return
        }

        setContent {
            PoscheduleTheme {
                ImportDraftScreen(
                    sharePayload = sharePayload,
                    onSuccess = {
                        // Show success toast
                        Toast.makeText(
                            this@ShareReceiverActivity,
                            "일정이 추가되었습니다",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Plan saved - navigate to TodayScreen to show the new plan
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    },
                    onCancel = {
                        // User cancelled - return to sharing app
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Extracts SharePayload from Android Intent.
     *
     * Handles:
     * - EXTRA_TEXT (String) for text content
     * - EXTRA_STREAM (Uri) for images
     * - Caller package name for analytics
     */
    private fun parseShareIntent(intent: Intent): SharePayload? {
        if (intent.action != Intent.ACTION_SEND) {
            return null
        }

        val type = intent.type ?: return null
        val source = callingPackage // May be null on some Android versions

        return when {
            type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                SharePayload.SharedText(text = text, source = source)
            }
            type.startsWith("image/") -> {
                val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) ?: return null
                SharePayload.SharedImage(uri = uri, source = source)
            }
            else -> null
        }
    }
}
