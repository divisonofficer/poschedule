package com.jnkim.poschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jnkim.poschedule.data.local.entity.LlmResponseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LLM response logging.
 *
 * Provides methods to:
 * - Insert new LLM responses (both successful and failed)
 * - Query recent responses for export
 * - Filter failed parsing attempts for debugging
 * - Clean up old logs to manage storage
 */
@Dao
interface LlmResponseDao {
    /**
     * Insert a new LLM response log entry.
     * Replaces existing entry if ID conflicts (should rarely happen with UUID).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: LlmResponseEntity)

    /**
     * Get recent responses ordered by timestamp (newest first).
     *
     * @param limit Maximum number of responses to return (default 50)
     * @return Flow of response entities for reactive updates
     */
    @Query("SELECT * FROM llm_responses ORDER BY requestedAt DESC LIMIT :limit")
    fun getRecentResponses(limit: Int = 50): Flow<List<LlmResponseEntity>>

    /**
     * Get all failed parsing attempts for debugging.
     * Returns only responses where deserialization failed (parseSuccess = false).
     *
     * @return Flow of failed response entities, ordered newest first
     */
    @Query("SELECT * FROM llm_responses WHERE parseSuccess = 0 ORDER BY requestedAt DESC")
    fun getFailedParses(): Flow<List<LlmResponseEntity>>

    /**
     * Get responses within a date range.
     * Useful for exporting logs from a specific time period.
     *
     * @param afterMillis Start timestamp (inclusive)
     * @param beforeMillis End timestamp (exclusive)
     * @return List of responses in the specified range, ordered newest first
     */
    @Query("SELECT * FROM llm_responses WHERE requestedAt >= :afterMillis AND requestedAt < :beforeMillis ORDER BY requestedAt DESC")
    suspend fun getResponsesInRange(afterMillis: Long, beforeMillis: Long): List<LlmResponseEntity>

    /**
     * Delete old responses to prevent unbounded growth.
     * Recommended to run periodically (e.g., keep last 30 days).
     *
     * @param beforeMillis Delete responses older than this timestamp
     * @return Number of rows deleted
     */
    @Query("DELETE FROM llm_responses WHERE requestedAt < :beforeMillis")
    suspend fun deleteResponsesBefore(beforeMillis: Long): Int

    /**
     * Get count of total stored responses.
     * Useful for monitoring storage usage.
     *
     * @return Total number of LLM responses in database
     */
    @Query("SELECT COUNT(*) FROM llm_responses")
    suspend fun getResponseCount(): Int

    /**
     * Get count of failed parses.
     * Useful for calculating success rate.
     *
     * @return Number of responses that failed to parse
     */
    @Query("SELECT COUNT(*) FROM llm_responses WHERE parseSuccess = 0")
    suspend fun getFailedParseCount(): Int
}
