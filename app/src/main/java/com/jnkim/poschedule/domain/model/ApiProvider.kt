package com.jnkim.poschedule.domain.model

/**
 * API provider options for LLM services.
 */
enum class ApiProvider {
    POSTECH,  // POSTECH GenAI API (requires POSTECH authentication)
    GEMINI    // Google Gemini API (requires user-managed API key)
}

/**
 * Model options available for POSTECH GenAI API.
 * Each model corresponds to a specific endpoint path.
 */
enum class PostechModel(val path: String) {
    GPT("a1/gpt"),
    GEMINI("a2/gemini"),
    CLAUDE("a3/claude")
}
