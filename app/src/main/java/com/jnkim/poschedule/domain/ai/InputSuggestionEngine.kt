package com.jnkim.poschedule.domain.ai

import com.jnkim.poschedule.domain.model.InputSuggestion
import com.jnkim.poschedule.domain.model.SuggestionCategory

/**
 * Static suggestion database for Korean natural language input.
 * Contains pre-defined patterns for time, task, and anchor expressions.
 */
object SuggestionDatabase {
    val TIME_PATTERNS = listOf(
        // 오늘/내일/모레
        InputSuggestion("오늘", "오늘", SuggestionCategory.TIME, "📅"),
        InputSuggestion("오늘 오후", "오늘 오후", SuggestionCategory.TIME, "📅"),
        InputSuggestion("오늘 저녁", "오늘 저녁", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("내일", "내일", SuggestionCategory.TIME, "📅"),
        InputSuggestion("내일 아침", "내일 아침", SuggestionCategory.TIME, "🌅"),
        InputSuggestion("내일 점심", "내일 점심", SuggestionCategory.TIME, "🍱"),
        InputSuggestion("내일 오후", "내일 오후", SuggestionCategory.TIME, "☀️"),
        InputSuggestion("내일 저녁", "내일 저녁", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("모레", "모레", SuggestionCategory.TIME, "📅"),
        InputSuggestion("모레 아침", "모레 아침", SuggestionCategory.TIME, "🌅"),

        // 오전 시간대
        InputSuggestion("오전 8시", "오전 8시", SuggestionCategory.TIME, "☀️"),
        InputSuggestion("오전 9시", "오전 9시", SuggestionCategory.TIME, "☀️"),
        InputSuggestion("오전 10시", "오전 10시", SuggestionCategory.TIME, "☀️"),
        InputSuggestion("오전 11시", "오전 11시", SuggestionCategory.TIME, "☀️"),

        // 오후 시간대
        InputSuggestion("오후 1시", "오후 1시", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("오후 2시", "오후 2시", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("오후 3시", "오후 3시", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("오후 4시", "오후 4시", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("오후 5시", "오후 5시", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("오후 6시", "오후 6시", SuggestionCategory.TIME, "⏰"),

        // 저녁 시간대
        InputSuggestion("저녁 7시", "저녁 7시", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("저녁 8시", "저녁 8시", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("저녁 9시", "저녁 9시", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("밤 10시", "밤 10시", SuggestionCategory.TIME, "🌙"),

        // 이번 주
        InputSuggestion("이번 주", "이번 주", SuggestionCategory.DAY, "📆"),
        InputSuggestion("이번 주말", "이번 주말", SuggestionCategory.DAY, "📆"),
        InputSuggestion("이번 주 월요일", "이번 주 월요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("이번 주 화요일", "이번 주 화요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("이번 주 수요일", "이번 주 수요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("이번 주 목요일", "이번 주 목요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("이번 주 금요일", "이번 주 금요일", SuggestionCategory.DAY, "📅"),

        // 다음 주
        InputSuggestion("다음 주", "다음 주", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주 월요일", "다음 주 월요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주 화요일", "다음 주 화요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주 수요일", "다음 주 수요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주 목요일", "다음 주 목요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주 금요일", "다음 주 금요일", SuggestionCategory.DAY, "📅"),
        InputSuggestion("다음 주말", "다음 주말", SuggestionCategory.DAY, "📆")
    )

    val TASK_PATTERNS = listOf(
        // 학업 관련
        InputSuggestion("과제 제출", "과제 제출", SuggestionCategory.TASK_TYPE, "📝"),
        InputSuggestion("과제 하기", "과제 하기", SuggestionCategory.TASK_TYPE, "📝"),
        InputSuggestion("숙제 하기", "숙제 하기", SuggestionCategory.TASK_TYPE, "✏️"),
        InputSuggestion("공부하기", "공부하기", SuggestionCategory.TASK_TYPE, "📚"),
        InputSuggestion("수업 듣기", "수업 듣기", SuggestionCategory.TASK_TYPE, "🎓"),
        InputSuggestion("강의 듣기", "강의 듣기", SuggestionCategory.TASK_TYPE, "🎓"),
        InputSuggestion("리포트 작성", "리포트 작성", SuggestionCategory.TASK_TYPE, "📄"),
        InputSuggestion("레포트 쓰기", "레포트 쓰기", SuggestionCategory.TASK_TYPE, "📄"),
        InputSuggestion("발표 준비", "발표 준비", SuggestionCategory.TASK_TYPE, "🎤"),
        InputSuggestion("발표하기", "발표하기", SuggestionCategory.TASK_TYPE, "🎤"),
        InputSuggestion("시험 공부", "시험 공부", SuggestionCategory.TASK_TYPE, "✏️"),
        InputSuggestion("시험 보기", "시험 보기", SuggestionCategory.TASK_TYPE, "✏️"),
        InputSuggestion("프로젝트 진행", "프로젝트 진행", SuggestionCategory.TASK_TYPE, "💼"),
        InputSuggestion("프로젝트 회의", "프로젝트 회의", SuggestionCategory.TASK_TYPE, "💼"),
        InputSuggestion("팀플하기", "팀플하기", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("책 읽기", "책 읽기", SuggestionCategory.TASK_TYPE, "📖"),

        // 업무 관련
        InputSuggestion("회의 참석", "회의 참석", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("회의하기", "회의하기", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("미팅하기", "미팅하기", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("업무 보고", "업무 보고", SuggestionCategory.TASK_TYPE, "📋"),
        InputSuggestion("문서 작성", "문서 작성", SuggestionCategory.TASK_TYPE, "📄"),

        // 개인 생활
        InputSuggestion("운동하기", "운동하기", SuggestionCategory.TASK_TYPE, "💪"),
        InputSuggestion("헬스하기", "헬스하기", SuggestionCategory.TASK_TYPE, "💪"),
        InputSuggestion("산책하기", "산책하기", SuggestionCategory.TASK_TYPE, "🚶"),
        InputSuggestion("조깅하기", "조깅하기", SuggestionCategory.TASK_TYPE, "🏃"),
        InputSuggestion("요가하기", "요가하기", SuggestionCategory.TASK_TYPE, "🧘"),
        InputSuggestion("쇼핑하기", "쇼핑하기", SuggestionCategory.TASK_TYPE, "🛒"),
        InputSuggestion("청소하기", "청소하기", SuggestionCategory.TASK_TYPE, "🧹"),
        InputSuggestion("빨래하기", "빨래하기", SuggestionCategory.TASK_TYPE, "👕"),
        InputSuggestion("요리하기", "요리하기", SuggestionCategory.TASK_TYPE, "🍳"),

        // 사회 활동
        InputSuggestion("친구 만나기", "친구 만나기", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("약속 잡기", "약속 잡기", SuggestionCategory.TASK_TYPE, "📅"),
        InputSuggestion("전화하기", "전화하기", SuggestionCategory.TASK_TYPE, "📞"),
        InputSuggestion("영화 보기", "영화 보기", SuggestionCategory.TASK_TYPE, "🎬"),
        InputSuggestion("카페 가기", "카페 가기", SuggestionCategory.TASK_TYPE, "☕")
    )

    val ANCHOR_PATTERNS = listOf(
        // 식사 기준
        InputSuggestion("아침 먹고", "아침 먹고", SuggestionCategory.ANCHOR, "🌅"),
        InputSuggestion("점심 먹고", "점심 먹고", SuggestionCategory.ANCHOR, "🍱"),
        InputSuggestion("저녁 먹고", "저녁 먹고", SuggestionCategory.ANCHOR, "🍽️"),
        InputSuggestion("식사 후", "식사 후", SuggestionCategory.ANCHOR, "🍴"),

        // 학업 활동 기준
        InputSuggestion("수업 끝나고", "수업 끝나고", SuggestionCategory.ANCHOR, "🎓"),
        InputSuggestion("강의 끝나고", "강의 끝나고", SuggestionCategory.ANCHOR, "🎓"),
        InputSuggestion("수업 전에", "수업 전에", SuggestionCategory.ANCHOR, "🎓"),
        InputSuggestion("시험 끝나고", "시험 끝나고", SuggestionCategory.ANCHOR, "✏️"),

        // 업무/회의 기준
        InputSuggestion("회의 끝나고", "회의 끝나고", SuggestionCategory.ANCHOR, "👥"),
        InputSuggestion("회의 전에", "회의 전에", SuggestionCategory.ANCHOR, "👥"),
        InputSuggestion("미팅 후", "미팅 후", SuggestionCategory.ANCHOR, "👥"),
        InputSuggestion("퇴근 후", "퇴근 후", SuggestionCategory.ANCHOR, "🏃"),
        InputSuggestion("출근 전", "출근 전", SuggestionCategory.ANCHOR, "🏢"),

        // 운동/활동 기준
        InputSuggestion("운동 끝나고", "운동 끝나고", SuggestionCategory.ANCHOR, "💪"),
        InputSuggestion("운동 전에", "운동 전에", SuggestionCategory.ANCHOR, "💪"),
        InputSuggestion("헬스 끝나고", "헬스 끝나고", SuggestionCategory.ANCHOR, "💪"),
        InputSuggestion("산책 후", "산책 후", SuggestionCategory.ANCHOR, "🚶"),

        // 일상 생활 기준
        InputSuggestion("일어나서", "일어나서", SuggestionCategory.ANCHOR, "☀️"),
        InputSuggestion("일어난 후", "일어난 후", SuggestionCategory.ANCHOR, "☀️"),
        InputSuggestion("자기 전에", "자기 전에", SuggestionCategory.ANCHOR, "🌙"),
        InputSuggestion("샤워 후", "샤워 후", SuggestionCategory.ANCHOR, "🚿"),
        InputSuggestion("씻고 나서", "씻고 나서", SuggestionCategory.ANCHOR, "🚿"),

        // 집 출입 기준
        InputSuggestion("집 가서", "집 가서", SuggestionCategory.ANCHOR, "🏠"),
        InputSuggestion("집에 도착해서", "집에 도착해서", SuggestionCategory.ANCHOR, "🏠"),
        InputSuggestion("외출 전", "외출 전", SuggestionCategory.ANCHOR, "🚪"),

        // 학교 기준
        InputSuggestion("학교 가서", "학교 가서", SuggestionCategory.ANCHOR, "🏫"),
        InputSuggestion("학교 끝나고", "학교 끝나고", SuggestionCategory.ANCHOR, "🏫")
    )

    val ENGLISH_TIME_PATTERNS = listOf(
        InputSuggestion("3 PM", "3 PM", SuggestionCategory.TIME, "⏰"),
        InputSuggestion("tomorrow morning", "tomorrow morning", SuggestionCategory.TIME, "🌅"),
        InputSuggestion("7 PM", "7 PM", SuggestionCategory.TIME, "🌙"),
        InputSuggestion("10 AM", "10 AM", SuggestionCategory.TIME, "☀️"),
        InputSuggestion("next Monday", "next Monday", SuggestionCategory.DAY, "📅"),
        InputSuggestion("this weekend", "this weekend", SuggestionCategory.DAY, "📆")
    )

    val ENGLISH_TASK_PATTERNS = listOf(
        InputSuggestion("submit assignment", "submit assignment", SuggestionCategory.TASK_TYPE, "📝"),
        InputSuggestion("attend meeting", "attend meeting", SuggestionCategory.TASK_TYPE, "👥"),
        InputSuggestion("exercise", "exercise", SuggestionCategory.TASK_TYPE, "💪"),
        InputSuggestion("study", "study", SuggestionCategory.TASK_TYPE, "📚"),
        InputSuggestion("read book", "read book", SuggestionCategory.TASK_TYPE, "📖")
    )

    val ENGLISH_ANCHOR_PATTERNS = listOf(
        InputSuggestion("after lunch", "after lunch", SuggestionCategory.ANCHOR, "🍱"),
        InputSuggestion("after class", "after class", SuggestionCategory.ANCHOR, "🎓"),
        InputSuggestion("after dinner", "after dinner", SuggestionCategory.ANCHOR, "🍽️")
    )
}

/**
 * Engine for generating context-aware input suggestions.
 * Uses static database without API calls for fast, offline suggestions.
 */
class InputSuggestionEngine {

    /**
     * Generates 3 suggestions based on current input.
     * Automatically detects language from input text.
     *
     * @param currentText The user's current input text
     * @param locale Language code ("ko" for Korean, "en" for English) - auto-detected if not specified
     * @return List of up to 3 relevant suggestions
     */
    fun generateSuggestions(
        currentText: String,
        locale: String = "ko"
    ): List<InputSuggestion> {
        val normalized = currentText.trim().lowercase()

        // Auto-detect locale from input text
        val detectedLocale = if (normalized.isEmpty()) {
            locale // Use provided locale for empty input
        } else {
            detectLocale(normalized)
        }

        // Select patterns based on detected locale
        val timePatterns = if (detectedLocale == "ko") {
            SuggestionDatabase.TIME_PATTERNS
        } else {
            SuggestionDatabase.ENGLISH_TIME_PATTERNS
        }

        val taskPatterns = if (detectedLocale == "ko") {
            SuggestionDatabase.TASK_PATTERNS
        } else {
            SuggestionDatabase.ENGLISH_TASK_PATTERNS
        }

        val anchorPatterns = if (detectedLocale == "ko") {
            SuggestionDatabase.ANCHOR_PATTERNS
        } else {
            SuggestionDatabase.ENGLISH_ANCHOR_PATTERNS
        }

        // Empty input: show diverse examples (one from each category)
        if (normalized.isEmpty()) {
            return listOf(
                timePatterns.random(),
                taskPatterns.random(),
                anchorPatterns.random()
            )
        }

        // First, try to find suggestions that start with the input text
        val allPatterns = timePatterns + taskPatterns + anchorPatterns

        // Try matching with full input first
        var startsWith = allPatterns.filter { suggestion ->
            val suggestionLower = suggestion.text.lowercase()
            val insertLower = suggestion.insertText.lowercase()

            // Direct starts with matching
            suggestionLower.startsWith(normalized) || insertLower.startsWith(normalized) ||
            // Also match if suggestion contains the text after a space (for partial word matching)
            suggestionLower.contains(" $normalized") || insertLower.contains(" $normalized")
        }

        // If full input didn't match, try matching with last word only
        if (startsWith.isEmpty() && normalized.contains(" ")) {
            val lastWord = normalized.substringAfterLast(" ").trim()
            if (lastWord.isNotEmpty()) {
                startsWith = allPatterns.filter { suggestion ->
                    val suggestionLower = suggestion.text.lowercase()
                    val insertLower = suggestion.insertText.lowercase()
                    suggestionLower.startsWith(lastWord) || insertLower.startsWith(lastWord)
                }
            }
        }

        // If we found suggestions that start with input, use those
        if (startsWith.isNotEmpty()) {
            return startsWith.take(3)
        }

        // For single character Korean input (including Jamo), show all relevant patterns
        if (normalized.length == 1 && detectedLocale == "ko") {
            // Show a mix from all categories for single character input
            val mixed = mutableListOf<InputSuggestion>()
            mixed.addAll(timePatterns.shuffled().take(1))
            mixed.addAll(taskPatterns.shuffled().take(1))
            mixed.addAll(anchorPatterns.shuffled().take(1))
            return mixed.take(3)
        }

        // Otherwise, fall back to keyword-based filtering
        val filtered = mutableListOf<InputSuggestion>()

        when {
            containsTimeKeywords(normalized, detectedLocale) -> {
                // User is typing time-related text, show time suggestions
                filtered.addAll(timePatterns.take(3))
            }
            containsTaskKeywords(normalized, detectedLocale) -> {
                // User is typing task-related text, show task suggestions
                filtered.addAll(taskPatterns.take(3))
            }
            containsAnchorKeywords(normalized, detectedLocale) -> {
                // User is typing anchor-related text, show anchor suggestions
                filtered.addAll(anchorPatterns.take(3))
            }
            else -> {
                // Mix suggestions from all categories
                filtered.add(timePatterns.random())
                filtered.add(taskPatterns.random())
                filtered.add(anchorPatterns.random())
            }
        }

        return filtered.take(3)
    }

    /**
     * Detects the locale of the input text.
     * Returns "ko" if Korean characters are present, otherwise "en".
     */
    private fun detectLocale(text: String): String {
        // Korean Unicode range: \uAC00-\uD7A3 (Hangul Syllables)
        // Korean Jamo: \u1100-\u11FF, \u3130-\u318F
        val hasKorean = text.any { char ->
            char in '\uAC00'..'\uD7A3' || // Hangul syllables (가-힣)
            char in '\u1100'..'\u11FF' || // Hangul Jamo
            char in '\u3130'..'\u318F'    // Hangul compatibility Jamo
        }
        return if (hasKorean) "ko" else "en"
    }

    /**
     * Checks if text contains time-related keywords.
     * Supports partial matching for English (e.g., "tom" matches "tomorrow").
     */
    private fun containsTimeKeywords(text: String, locale: String): Boolean {
        val koKeywords = listOf(
            // 시간
            "시", "분", "오전", "오후", "아침", "점심", "저녁", "밤",
            // 날짜
            "오늘", "내일", "모레", "글피",
            // 주/주말
            "주말", "이번", "다음", "주",
            // 요일
            "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일",
            "월", "화", "수", "목", "금", "토", "일"
        )

        val enKeywords = listOf(
            "am", "pm", "morning", "afternoon", "evening",
            "tomorrow", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "weekend"
        )

        val keywords = if (locale == "ko") koKeywords else enKeywords

        // For English, support partial matching (e.g., "tom" matches "tomorrow")
        return if (locale == "en" && text.length >= 2) {
            keywords.any { keyword -> keyword.startsWith(text) || text.contains(keyword) }
        } else {
            keywords.any { text.contains(it) }
        }
    }

    /**
     * Checks if text contains task-related keywords.
     * Supports partial matching for English (e.g., "meet" matches "meeting").
     */
    private fun containsTaskKeywords(text: String, locale: String): Boolean {
        val koKeywords = listOf(
            // 학업
            "과제", "숙제", "공부", "수업", "강의", "리포트", "레포트",
            "발표", "시험", "프로젝트", "팀플", "책",
            // 업무
            "회의", "미팅", "업무", "보고", "문서",
            // 운동
            "운동", "헬스", "산책", "조깅", "요가",
            // 생활
            "쇼핑", "청소", "빨래", "요리",
            // 사회
            "친구", "약속", "전화", "영화", "카페"
        )

        val enKeywords = listOf(
            "assignment", "homework", "study", "meeting",
            "exercise", "class", "report", "presentation",
            "exam", "project", "book"
        )

        val keywords = if (locale == "ko") koKeywords else enKeywords

        // For English, support partial matching (e.g., "meet" matches "meeting")
        return if (locale == "en" && text.length >= 2) {
            keywords.any { keyword -> keyword.startsWith(text) || text.contains(keyword) }
        } else {
            keywords.any { text.contains(it) }
        }
    }

    /**
     * Checks if text contains anchor-related keywords.
     * Supports partial matching for English (e.g., "aft" matches "after").
     */
    private fun containsAnchorKeywords(text: String, locale: String): Boolean {
        val koKeywords = listOf(
            // 식사
            "먹고", "아침", "점심", "저녁", "식사",
            // 학업
            "수업", "강의", "시험", "끝나고", "전에", "전", "후",
            // 업무
            "회의", "미팅", "퇴근", "출근",
            // 운동
            "운동", "헬스", "산책",
            // 일상
            "일어나서", "일어난", "자기", "샤워", "씻고",
            // 장소
            "집", "학교", "외출", "도착"
        )

        val enKeywords = listOf(
            "after", "lunch", "dinner", "breakfast", "class"
        )

        val keywords = if (locale == "ko") koKeywords else enKeywords

        // For English, support partial matching (e.g., "aft" matches "after")
        return if (locale == "en" && text.length >= 2) {
            keywords.any { keyword -> keyword.startsWith(text) || text.contains(keyword) }
        } else {
            keywords.any { text.contains(it) }
        }
    }
}
