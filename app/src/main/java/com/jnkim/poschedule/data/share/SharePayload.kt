package com.jnkim.poschedule.data.share

import android.net.Uri

/**
 * Sealed class representing content shared from external apps.
 *
 * Phase 1: TEXT only
 * Phase 2: Add URL, IMAGE
 */
sealed class SharePayload {
    /**
     * Plain text shared from messages, emails, notes, etc.
     * @param text The shared text content
     * @param source Package name of the sharing app (e.g., "com.kakao.talk")
     */
    data class SharedText(
        val text: String,
        val source: String?
    ) : SharePayload()

    /**
     * Image shared from screenshots, photos, etc.
     * @param uri Content URI of the shared image
     * @param source Package name of the sharing app
     */
    data class SharedImage(
        val uri: Uri,
        val source: String?
    ) : SharePayload()

    // Phase 3: URL share with web scraping
    // data class SharedUrl(
    //     val url: String,
    //     val source: String?,
    //     val contextText: String? = null
    // ) : SharePayload()
}
