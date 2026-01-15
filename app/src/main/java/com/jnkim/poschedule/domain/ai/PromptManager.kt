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
            - Return format: JSON array of objects with keys 'title', 'etaMin' (int), 'effort' (low/medium), and 'iconEmoji' (string).
            - Focus on the very first physical steps.
            - Generate a relevant emoji icon for each task that visually represents the action.
              Examples: 🧹 cleaning, 📚 organizing books, 💻 computer work, 🍽️ dishes,
                       🗑️ trash, 📦 organizing boxes, 🧺 laundry, 🪴 plants, etc.

            Example output format:
            [
              {
                "title": "Clear desk surface and put items in proper places",
                "etaMin": 3,
                "effort": "low",
                "iconEmoji": "🧹"
              },
              {
                "title": "Organize papers and books into categories",
                "etaMin": 4,
                "effort": "medium",
                "iconEmoji": "📚"
              },
              {
                "title": "Wipe down desk with cleaning cloth",
                "etaMin": 2,
                "effort": "low",
                "iconEmoji": "✨"
              }
            ]
        """.trimIndent()
    }

    fun getLLMTaskNormalizerPrompt(locale: String): String {
        val langName = if (locale == "ko") "Korean" else "English"
        // Get current date for one-time event examples
        val today = java.time.LocalDate.now()
        val nextWeekDate = today.plusDays(7).toString()  // yyyy-MM-dd

        val examples = if (locale == "ko") {
            """
            Example 1 (Recurring - Daily):
            User: "아침 약 먹기, 기상 후 30분"
            Output: {"intent":"create_plan","plan":{"title":"아침 약 먹기","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊","time":{"anchor":"WAKE","offset":30,"duration":5},"recurrence":{"kind":"DAILY"},"specificDate":null},"confidence":0.9}

            Example 2 (Recurring - Weekly):
            User: "매주 월수금 운동, 저녁 7시"
            Output: {"intent":"create_plan","plan":{"title":"운동","planType":"ROUTINE","routineType":"MOVEMENT","iconEmoji":"🏃","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[1,3,5]},"specificDate":null},"confidence":0.85}

            Example 3 (Recurring - Weekly):
            User: "피아노 레슨 매주 화요일 2시"
            Output: {"intent":"create_plan","plan":{"title":"피아노 레슨","planType":"TASK","iconEmoji":"🎹","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[2]},"specificDate":null},"confidence":0.9}

            Example 4 (One-time - Specific Date):
            User: "16일 금요일 오후2시에 교수님과 연구미팅"
            Output: {"intent":"create_plan","plan":{"title":"교수님과 연구미팅","planType":"MEETING","iconEmoji":"📊","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"NONE"},"specificDate":"2026-01-16"},"confidence":0.95}

            Example 5 (One-time - This Friday):
            User: "이번 주 금요일 저녁에 야구 시청"
            Output: {"intent":"create_plan","plan":{"title":"야구 시청","planType":"TASK","iconEmoji":"⚾","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":180},"recurrence":{"kind":"NONE"},"specificDate":"$nextWeekDate"},"confidence":0.8,"clarifications":[],"alternatives":[]}

            Example 6 (Multiple tasks with alternatives):
            User: "난 밤 11시 30분엔 꼭 잘꺼야 근데 그전에 6시에 유산균먹어야돼"
            Output: {"intent":"create_plan","plan":{"title":"취침 준비","planType":"ROUTINE","routineType":"WIND_DOWN","iconEmoji":"🌙","time":{"anchor":"FIXED","fixedHour":23,"fixedMinute":30,"duration":30},"recurrence":{"kind":"DAILY"},"specificDate":null},"confidence":0.9,"clarifications":[],"alternatives":[{"title":"유산균 복용","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊"}]}
            """.trimIndent()
        } else {
            """
            Example 1 (Recurring - Daily):
            User: "Take morning meds, 30 min after wake"
            Output: {"intent":"create_plan","plan":{"title":"Take morning meds","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊","time":{"anchor":"WAKE","offset":30,"duration":5},"recurrence":{"kind":"DAILY"},"specificDate":null},"confidence":0.9}

            Example 2 (Recurring - Weekly):
            User: "Piano lessons every Tuesday at 2pm"
            Output: {"intent":"create_plan","plan":{"title":"Piano lessons","planType":"TASK","iconEmoji":"🎹","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"WEEKLY","weekdays":[2]},"specificDate":null},"confidence":0.9}

            Example 3 (One-time - Specific Date):
            User: "Meeting with professor on Jan 16 at 2pm"
            Output: {"intent":"create_plan","plan":{"title":"Meeting with professor","planType":"MEETING","iconEmoji":"📊","time":{"anchor":"FIXED","fixedHour":14,"fixedMinute":0,"duration":60},"recurrence":{"kind":"NONE"},"specificDate":"2026-01-16"},"confidence":0.95}

            Example 4 (One-time - This Friday):
            User: "Watch baseball game this Friday night"
            Output: {"intent":"create_plan","plan":{"title":"Watch baseball game","planType":"TASK","iconEmoji":"⚾","time":{"anchor":"FIXED","fixedHour":19,"fixedMinute":0,"duration":180},"recurrence":{"kind":"NONE"},"specificDate":"$nextWeekDate"},"confidence":0.8,"clarifications":[],"alternatives":[]}

            Example 5 (Multiple tasks with alternatives):
            User: "I need to sleep at 11:30 PM but I should take probiotics at 6 AM"
            Output: {"intent":"create_plan","plan":{"title":"Bedtime routine","planType":"ROUTINE","routineType":"WIND_DOWN","iconEmoji":"🌙","time":{"anchor":"FIXED","fixedHour":23,"fixedMinute":30,"duration":30},"recurrence":{"kind":"DAILY"},"specificDate":null},"confidence":0.9,"clarifications":[],"alternatives":[{"title":"Take probiotics","planType":"ROUTINE","routineType":"MEDS_AM","iconEmoji":"💊"}]}
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
                "specificDate": "string or null (yyyy-MM-dd format, REQUIRED when recurrence.kind = NONE)",
                "note": "string or null"
              },
              "confidence": 0.0 to 1.0,
              "clarifications": ["questions if ambiguous"],
              "alternatives": [
                {
                  "title": "string",
                  "planType": "ROUTINE|TASK|CHORE|MEETING",
                  "routineType": "string or null",
                  "iconEmoji": "string or null"
                }
              ]
            }

            CRITICAL RULES:
            - The "time" field is REQUIRED and must always be present in the "plan" object
            - Never use "steps" array
            - When recurrence.kind = "NONE" (one-time event), specificDate MUST be provided in yyyy-MM-dd format
            - When recurrence.kind is not "NONE" (recurring event), specificDate MUST be null
            - TODAY'S DATE is ${java.time.LocalDate.now()} - use this to calculate specific dates from relative references
            - ALTERNATIVES MUST BE OBJECTS: Each item in the "alternatives" array MUST be an object with fields {title, planType, routineType, iconEmoji}, NOT a plain string

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
            - recurrence.kind: NONE (one-time), DAILY, WEEKLY, WEEKDAYS, MONTHLY
            - specificDate: REQUIRED when kind=NONE, null otherwise. Format: yyyy-MM-dd
            - duration: default 15 minutes unless specified
            - Clamp offset: -180 to +720 minutes
            - Clamp fixedHour: 0-23, fixedMinute: 0-59
            - If user describes multiple activities, combine them into ONE task title or pick the most important one
            - alternatives: If user mentions multiple tasks, put the MAIN task in "plan" and other tasks in "alternatives" array as objects with {title, planType, routineType, iconEmoji}. Leave empty array [] if only one task.

            Date Parsing Guidelines:
            - "16일" or "16th" = calculate the specific date (e.g., 2026-01-16 if current month is January)
            - "이번 주 금요일" or "this Friday" = calculate the date of the upcoming Friday
            - "내일" or "tomorrow" = current date + 1 day
            - "다음 주" or "next week" = current date + 7 days
            - Always output dates in yyyy-MM-dd format

            Deadline/Notification Extraction:
            - If the input contains notification text (emails, messages, etc.) with a deadline/due date, EXTRACT it
            - Keywords to look for: "deadline", "due", "by", "before", "까지", "마감", "제출"
            - Example: "submission form deadline, 10pm (GMT) Jan 22, 2026" → create task "Submission deadline" on 2026-01-22
            - IGNORE irrelevant details (URLs, instructions, passwords) and focus on the TIME-SENSITIVE information
            - If a clear deadline date is mentioned anywhere in the text, treat it as a valid scheduling request

            $examples

            Safety:
            - No medical advice (dosages, prescriptions)
            - No financial advice
            - Only return {"intent":"unsure","confidence":0.0} if there is ABSOLUTELY NO time/date/deadline information in the input

            Output JSON:
        """.trimIndent()
    }
}
