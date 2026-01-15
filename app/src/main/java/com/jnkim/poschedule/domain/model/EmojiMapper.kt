package com.jnkim.poschedule.domain.model

/**
 * Maps plan types and titles to fallback emoji icons.
 *
 * Used when no custom emoji is set for a plan. Provides instant
 * visual recognition for common plan types.
 *
 * Can be overridden by:
 * - LLM suggestions (future enhancement)
 * - User manual selection
 * - Custom emoji field in database
 *
 * Mapping Strategy:
 * 1. Try exact plan type match
 * 2. Fall back to keyword matching in title
 * 3. Use default fallback (📌)
 */
object EmojiMapper {

    /**
     * Plan type to emoji mapping.
     * Based on DESIGN_REFINEMENT_PLAN emoji icon system.
     */
    private val typeToEmoji = mapOf(
        // Medical & Health
        "MEDS" to "💊",
        "MEDICATION" to "💊",
        "MEDICINE" to "💊",
        "PILL" to "💊",
        "DOCTOR" to "🏥",
        "HOSPITAL" to "🏥",
        "HEALTH" to "❤️",
        "THERAPY" to "🧘",

        // Food & Nutrition
        "MEAL" to "🍽️",
        "BREAKFAST" to "🥐",
        "LUNCH" to "🍱",
        "DINNER" to "🍽️",
        "SNACK" to "🍪",
        "COOKING" to "🍳",
        "EATING" to "🍽️",
        "FOOD" to "🍽️",

        // Sleep & Rest
        "SLEEP" to "🌙",
        "NAP" to "😴",
        "WIND_DOWN" to "🌙",
        "REST" to "😴",
        "BEDTIME" to "🛏️",

        // Study & Learning
        "STUDY" to "📚",
        "READING" to "📖",
        "BOOK" to "📚",
        "HOMEWORK" to "📝",
        "PAPER" to "✍️",
        "WRITING" to "✍️",
        "RESEARCH" to "🔬",
        "LEARN" to "📚",
        "COURSE" to "🎓",
        "CLASS" to "🏫",

        // Work & Meetings
        "MEETING" to "🤝",
        "CALL" to "📞",
        "VIDEO" to "💻",
        "CONFERENCE" to "🧑‍🏫",
        "WORK" to "💼",
        "PROJECT" to "📊",
        "TASK" to "✅",
        "JOB" to "💼",

        // Exercise & Fitness
        "EXERCISE" to "🏃",
        "WORKOUT" to "💪",
        "GYM" to "🏋️",
        "RUN" to "🏃",
        "WALK" to "🚶",
        "YOGA" to "🧘",
        "SPORT" to "⚽",
        "FITNESS" to "💪",

        // Household & Chores
        "CLEANING" to "🧹",
        "LAUNDRY" to "🧺",
        "DISHES" to "🍽️",
        "VACUUM" to "🧹",
        "TIDY" to "🧹",
        "ORGANIZE" to "📦",

        // Shopping & Errands
        "SHOPPING" to "🛒",
        "GROCERY" to "🛒",
        "ERRAND" to "🏃",
        "BANK" to "🏦",
        "POST" to "📮",
        "PHARMACY" to "💊",

        // Social & Entertainment
        "SOCIAL" to "👥",
        "FRIEND" to "👥",
        "PARTY" to "🎉",
        "EVENT" to "🎊",
        "MOVIE" to "🎬",
        "MUSIC" to "🎵",
        "GAME" to "🎮",
        "HOBBY" to "🎨",

        // Transportation
        "COMMUTE" to "🚗",
        "DRIVE" to "🚗",
        "BUS" to "🚌",
        "TRAIN" to "🚆",
        "TRAVEL" to "✈️",

        // Personal Care
        "SHOWER" to "🚿",
        "BATH" to "🛁",
        "HAIRCUT" to "💇",
        "DENTAL" to "🦷",
        "GROOMING" to "✨",

        // Creative & Hobbies
        "ART" to "🎨",
        "PAINT" to "🖌️",
        "DRAW" to "✏️",
        "CRAFT" to "✂️",
        "PHOTO" to "📸",
        "MUSIC_PRACTICE" to "🎸",

        // Miscellaneous
        "REMINDER" to "⏰",
        "APPOINTMENT" to "📅",
        "CALL_BACK" to "📞",
        "EMAIL" to "📧",
        "PLAN" to "📋",
        "REVIEW" to "📝"
    )

    /**
     * Gets fallback emoji for a plan based on title and optional plan type.
     *
     * Matching Strategy:
     * 1. If planType provided, try exact match in typeToEmoji
     * 2. If no match, scan title for keywords
     * 3. Return default fallback if no matches found
     *
     * @param title Plan title (e.g., "Take morning medication")
     * @param planType Optional plan type enum name (e.g., "MEDS")
     * @return Emoji string (e.g., "💊")
     */
    fun getEmojiForPlan(title: String, planType: String? = null): String {
        // Try exact type match first
        planType?.let { type ->
            typeToEmoji[type.uppercase()]?.let { return it }
        }

        // Fallback to keyword matching in title
        val titleUpper = title.uppercase()

        // Try to find any keyword that matches
        typeToEmoji.entries.find { (key, _) ->
            titleUpper.contains(key)
        }?.value?.let { return it }

        // Default fallback
        return "📌"
    }

    /**
     * Gets multiple emoji suggestions for a plan.
     * Useful for UI where user can pick from alternatives.
     *
     * Currently returns single fallback + defaults.
     * Can be enhanced with LLM suggestions in future.
     *
     * @param title Plan title
     * @param planType Optional plan type
     * @return List of emoji suggestions (primary first)
     */
    fun getEmojiSuggestions(title: String, planType: String? = null): List<String> {
        val primary = getEmojiForPlan(title, planType)

        // Return primary + common alternatives
        return listOf(
            primary,
            "✅", // Generic task
            "⭐", // Important
            "📌", // Pin/reminder
            "🎯"  // Goal
        ).distinct()
    }

    /**
     * Checks if a string is a valid emoji character.
     * Simple check - can be enhanced for full emoji validation.
     *
     * @param text String to check
     * @return True if appears to be emoji
     */
    fun isEmoji(text: String): Boolean {
        if (text.isEmpty()) return false

        // Simple heuristic: emojis are typically 1-2 chars and non-ASCII
        return text.length <= 4 && text.any { it.code > 127 }
    }
}
