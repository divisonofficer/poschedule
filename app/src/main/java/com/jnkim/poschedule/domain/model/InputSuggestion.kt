package com.jnkim.poschedule.domain.model

/**
 * Represents a suggestion for the LLM task input field.
 *
 * @param text Display text shown to the user (e.g., "오후 3시에")
 * @param insertText Text to be inserted when the suggestion is tapped
 * @param category Category of the suggestion for context-aware filtering
 * @param emoji Optional emoji prefix (e.g., "⏰")
 */
data class InputSuggestion(
    val text: String,
    val insertText: String,
    val category: SuggestionCategory,
    val emoji: String? = null
)

/**
 * Categories for input suggestions to enable context-aware filtering.
 */
enum class SuggestionCategory {
    /** Time-related expressions (e.g., "오후 3시에", "내일 아침") */
    TIME,

    /** Day/date expressions (e.g., "다음 주 월요일", "이번 주말") */
    DAY,

    /** Task type categories (e.g., "과제 제출", "회의 참석") */
    TASK_TYPE,

    /** Anchor-based time expressions (e.g., "점심 먹고", "수업 끝나고") */
    ANCHOR
}
