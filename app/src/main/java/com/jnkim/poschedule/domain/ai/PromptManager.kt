package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.domain.model.Mode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptManager @Inject constructor() {

    fun getGentleCopySystemPrompt(mode: Mode, locale: String): String {
        val langName = if (locale == "ko") "Korean" else "English"
        return """
            You are a supportive, calm OS assistant for a graduate student. 
            Current System Mode: $mode. 
            Task: Generate a supportive system message.
            Constraints:
            - Language: $langName
            - Return format: JSON with keys 'title' (max 20 chars) and 'body' (max 60 chars).
            - No medical advice. No pressure. No guilt.
            - Tone: Calm, academic, and friendly.
        """.trimIndent()
    }

    fun getTidySnapSystemPrompt(locale: String): String {
        val langName = if (locale == "ko") "Korean" else "English"
        return """
            You are a vision-based task analyst. 
            Task: Decompose a described chore into exactly 3 tiny micro-tasks.
            Constraints:
            - Language: $langName
            - Each task MUST take less than 5 minutes.
            - Return format: JSON array of objects with keys 'title', 'etaMin' (int), and 'effort' (low/medium).
            - Focus on the very first physical steps.
        """.trimIndent()
    }
}
