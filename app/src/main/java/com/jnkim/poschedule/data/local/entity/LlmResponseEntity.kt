package com.jnkim.poschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Database entity for storing LLM API responses.
 * Used for debugging JSON parsing errors and analyzing LLM response patterns.
 *
 * This entity stores both successful and failed LLM responses to help developers:
 * - Identify schema mismatches between expected and actual JSON structure
 * - Analyze common error patterns
 * - Improve system prompts based on actual LLM output
 * - Debug parsing failures with complete context
 */
@Entity(
    tableName = "llm_responses",
    indices = [
        androidx.room.Index(value = ["parseSuccess"]),
        androidx.room.Index(value = ["requestedAt"])
    ]
)
data class LlmResponseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    /**
     * Timestamp when the request was initiated (epoch milliseconds).
     * Used for sorting and cleanup of old logs.
     */
    val requestedAt: Long,

    /**
     * Original user input that triggered the LLM call.
     * Example: "아침 7시에 운동하기"
     */
    val userPrompt: String,

    /**
     * System prompt sent to the LLM.
     * Contains the schema definition and instructions for response format.
     */
    val systemPrompt: String,

    /**
     * Model identifier used for the request.
     * Example: "a3/claude"
     */
    val modelName: String,

    /**
     * Raw JSON response after cleaning (markdown/HTML removed).
     * This is the actual string that gets deserialized by kotlinx.serialization.
     * Stored for debugging when deserialization fails.
     */
    val rawResponse: String,

    /**
     * Whether the JSON response was successfully parsed into LLMTaskResponse.
     * true = deserialization succeeded, false = parsing failed
     */
    val parseSuccess: Boolean,

    /**
     * Error message if parsing failed.
     * Contains exception message and type for debugging.
     * null if parseSuccess = true
     */
    val errorMessage: String? = null,

    /**
     * Time taken to receive the response from LLM (milliseconds).
     * Useful for performance monitoring.
     */
    val responseTimeMs: Long
)
