package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.domain.model.Mode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptManager @Inject constructor() {

    fun getGentleCopySystemPrompt(mode: Mode, locale: String): String {
        val langName = if (locale == "ko") "Korean" else "English"

        val modeGuidance = when (mode) {
            Mode.RECOVERY -> "ultra-gentle tone, focus on rest and core routines only"
            Mode.LOW_MOOD -> "kind and patient, encourage tiny steps without pressure"
            Mode.BUSY -> "firm but supportive, minimize distractions"
            Mode.NORMAL -> "calm and encouraging, maintain steady momentum"
        }

        return """
You are a supportive OS assistant for a graduate student managing their routines.

Current System Mode: $mode
Guidance: $modeGuidance

Task: Generate a supportive system message based on the user's current state.

Output Format (strict JSON):
{
  "title": "string (max 20 characters)",
  "body": "string (max 60 characters)",
  "tone": "calm|firm|playful",
  "safetyTag": "optional string (e.g., 'no-medical-advice')"
}

Language: $langName

CRITICAL CONSTRAINTS:
- NO guilt language or pressure phrases
- NO suggestions for additional work beyond current plan
- NO medical advice, diagnosis, or clinical claims
- Focus on validation of progress and tiny achievable steps
- Respect user's current capacity and mode state

Examples of tone:
- RECOVERY: "Rest first", "Core routines only today"
- LOW_MOOD: "One tiny step", "You're doing enough"
- BUSY: "Focus mode active", "Essentials only"
- NORMAL: "Steady progress", "What's next?"
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

    fun getLLMTaskNormalizerPrompt(locale: String): String {
        val langName = if (locale == "ko") "Korean" else "English"
        val examples = if (locale == "ko") {
            """
            Example 1:
            User: "아침 약 먹기, 기상 후 30분"
            Output: {"intent":"create_plan","plan":{"title":"아침 약 먹기","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊","time":{"anchor":"WAKE","offset":30,"duration":5},"recurrence":{"kind":"DAILY"}},"confidence":0.9}

            Example 2:
            User: "매주 월수금 운동, 저녁 7시"
            Output: {"intent":"create_plan","plan":{"title":"운동","planType":"ROUTINE","routineType":"MOVEMENT","iconEmoji":"🏃","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[1,3,5]}},"confidence":0.85}

            Example 3:
            User: "피아노 레슨 매주 화요일 2시"
            Output: {"intent":"create_plan","plan":{"title":"피아노 레슨","planType":"TASK","iconEmoji":"🎹","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[2]}},"confidence":0.9}

            Example 4:
            User: "야구 시청 금요일 저녁"
            Output: {"intent":"create_plan","plan":{"title":"야구 시청","planType":"TASK","iconEmoji":"⚾","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":180},"recurrence":{"kind":"WEEKLY","weekdays":[5]}},"confidence":0.8}
            """.trimIndent()
        } else {
            """
            Example 1:
            User: "Take morning meds, 30 min after wake"
            Output: {"intent":"create_plan","plan":{"title":"Take morning meds","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊","time":{"anchor":"WAKE","offset":30,"duration":5},"recurrence":{"kind":"DAILY"}},"confidence":0.9}

            Example 2:
            User: "Piano lessons every Tuesday at 2pm"
            Output: {"intent":"create_plan","plan":{"title":"Piano lessons","planType":"TASK","iconEmoji":"🎹","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[2]}},"confidence":0.9}

            Example 3:
            User: "Watch baseball game Friday night"
            Output: {"intent":"create_plan","plan":{"title":"Watch baseball game","planType":"TASK","iconEmoji":"⚾","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":180},"recurrence":{"kind":"WEEKLY","weekdays":[5]}},"confidence":0.8}
            """.trimIndent()
        }

        return """
            You are a helpful assistant that converts natural language task descriptions into structured JSON for a personal schedule app.

            IMPORTANT: Create only ONE single task from the user input. If the user describes multiple tasks, pick the MAIN task or the FIRST task mentioned.

            Task: Parse the user's input and output a valid JSON object following this EXACT schema:
            {
              "version": "1.0",
              "language": "$locale",
              "intent": "create_plan" or "unsure",
              "plan": {
                "title": "string",
                "planType": "ROUTINE|TASK|CHORE|MEETING",
                "routineType": "string or null",
                "iconEmoji": "string or null",
                "importance": "MEDIUM",
                "time": {
                  "anchor": "WAKE|BED|FIXED|FLEX",
                  "fixedHour": "number or null",
                  "fixedMinute": "number or null",
                  "offset": "number or null",
                  "duration": "number",
                  "windowType": "SOFT"
                },
                "recurrence": {
                  "kind": "NONE|DAILY|WEEKLY|WEEKDAYS|MONTHLY",
                  "weekdays": "array or null",
                  "monthDay": "number or null"
                },
                "note": "string or null"
              },
              "confidence": 0.0 to 1.0,
              "clarifications": ["questions if ambiguous"],
              "alternatives": [alternative interpretations]
            }

            CRITICAL: The "time" field is REQUIRED and must always be present in the "plan" object. Never use "steps" array.

            Constraints:
            - Language: $langName
            - Return ONLY valid JSON, no markdown or explanations
            - If user input is ambiguous (confidence < 0.7), set intent="unsure" and include clarifications
            - planType: ROUTINE (recurring), TASK (one-time), CHORE (physical), MEETING
            - routineType: MEDS_AM, MEDS_PM, MEAL, MOVEMENT, STUDY, WIND_DOWN, CHORE (only if ROUTINE)
            - iconEmoji: REQUIRED - Generate a single relevant emoji that best represents the task
              Examples: 🎹 piano, 🏓 table tennis, ⚾ baseball, 🎸 guitar, 🏊 swimming, 🧘 yoga,
                       📚 study, 🍳 cooking, 🧹 cleaning, 🚗 driving, ✈️ travel, 🎮 gaming,
                       🎬 movies, 🎨 art, 💻 coding, 📝 writing, 🔬 lab work, 📊 presentation
            - anchor: WAKE (after waking), BED (before sleep), FIXED (specific time), FLEX (anytime)
            - recurrence.kind: NONE, DAILY, WEEKLY, WEEKDAYS, MONTHLY
            - duration: default 15 minutes unless specified
            - Clamp offset: -180 to +720 minutes
            - Clamp fixedHour: 0-23, fixedMinute: 0-59
            - If user describes multiple activities, combine them into ONE task title or pick the most important one

            $examples

            Safety:
            - No medical advice (dosages, prescriptions)
            - No financial advice
            - If input is unrelated to scheduling, return {"intent":"unsure","confidence":0.0}

            Output JSON:
        """.trimIndent()
    }
}
