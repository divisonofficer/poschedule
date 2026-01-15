package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.dao.LlmResponseDao
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing LLM response logs.
 * Provides export functionality to generate debug files.
 */
@Singleton
class LlmResponseRepository @Inject constructor(
    private val llmResponseDao: LlmResponseDao
) {
    /**
     * Export recent LLM responses to a formatted markdown file.
     *
     * @param limit Maximum number of responses to export (default 50)
     * @param outputDir Directory to write the file to
     * @return File object pointing to the exported file
     */
    suspend fun exportToMarkdown(
        limit: Int = 50,
        outputDir: File
    ): File {
        val responses = llmResponseDao.getResponsesInRange(
            afterMillis = 0,
            beforeMillis = System.currentTimeMillis()
        ).take(limit)

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            .format(Date())
        val filename = "llm_responses_$timestamp.md"
        val outputFile = File(outputDir, filename)

        outputFile.bufferedWriter().use { writer ->
            writer.write("# LLM Response Debug Log\n\n")
            writer.write("**Generated**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("**Total Responses**: ${responses.size}\n")
            writer.write("**Failed Parses**: ${responses.count { !it.parseSuccess }}\n\n")
            writer.write("---\n\n")

            responses.forEachIndexed { index, response ->
                writer.write("## Response ${index + 1}\n\n")
                writer.write("**Status**: ${if (response.parseSuccess) "✅ Success" else "❌ Failed"}\n")
                writer.write("**Timestamp**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(response.requestedAt))}\n")
                writer.write("**Response Time**: ${response.responseTimeMs}ms\n")
                writer.write("**Model**: ${response.modelName}\n\n")

                if (!response.parseSuccess && response.errorMessage != null) {
                    writer.write("**Error**: ${response.errorMessage}\n\n")
                }

                writer.write("### User Prompt\n")
                writer.write("```\n${response.userPrompt}\n```\n\n")

                writer.write("### System Prompt\n")
                writer.write("```\n${response.systemPrompt}\n```\n\n")

                writer.write("### Raw Response\n")
                writer.write("```json\n${formatJson(response.rawResponse)}\n```\n\n")

                writer.write("---\n\n")
            }
        }

        return outputFile
    }

    /**
     * Export recent LLM responses to a plain text file.
     *
     * @param limit Maximum number of responses to export (default 50)
     * @param outputDir Directory to write the file to
     * @return File object pointing to the exported file
     */
    suspend fun exportToText(
        limit: Int = 50,
        outputDir: File
    ): File {
        val responses = llmResponseDao.getResponsesInRange(
            afterMillis = 0,
            beforeMillis = System.currentTimeMillis()
        ).take(limit)

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            .format(Date())
        val filename = "llm_responses_$timestamp.txt"
        val outputFile = File(outputDir, filename)

        outputFile.bufferedWriter().use { writer ->
            writer.write("LLM RESPONSE DEBUG LOG\n")
            writer.write("=====================\n\n")
            writer.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("Total Responses: ${responses.size}\n")
            writer.write("Failed Parses: ${responses.count { !it.parseSuccess }}\n\n")
            writer.write("=====================\n\n")

            responses.forEachIndexed { index, response ->
                writer.write("RESPONSE ${index + 1}\n")
                writer.write("-----------\n")
                writer.write("Status: ${if (response.parseSuccess) "SUCCESS" else "FAILED"}\n")
                writer.write("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(response.requestedAt))}\n")
                writer.write("Response Time: ${response.responseTimeMs}ms\n")
                writer.write("Model: ${response.modelName}\n")

                if (!response.parseSuccess && response.errorMessage != null) {
                    writer.write("Error: ${response.errorMessage}\n")
                }

                writer.write("\n--- User Prompt ---\n")
                writer.write(response.userPrompt)
                writer.write("\n\n--- System Prompt ---\n")
                writer.write(response.systemPrompt)
                writer.write("\n\n--- Raw Response ---\n")
                writer.write(formatJson(response.rawResponse))
                writer.write("\n\n=====================\n\n")
            }
        }

        return outputFile
    }

    /**
     * Export only failed parsing attempts to a markdown file.
     * Useful for focused debugging of schema mismatches.
     *
     * @param outputDir Directory to write the file to
     * @return File object pointing to the exported file, or null if no failures found
     */
    suspend fun exportFailedParsesToMarkdown(outputDir: File): File? {
        val failedCount = llmResponseDao.getFailedParseCount()
        if (failedCount == 0) {
            return null
        }

        // Get all failed parses by querying within full time range
        val allResponses = llmResponseDao.getResponsesInRange(
            afterMillis = 0,
            beforeMillis = System.currentTimeMillis()
        )
        val failedResponses = allResponses.filter { !it.parseSuccess }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            .format(Date())
        val filename = "llm_failures_$timestamp.md"
        val outputFile = File(outputDir, filename)

        outputFile.bufferedWriter().use { writer ->
            writer.write("# LLM Parsing Failures\n\n")
            writer.write("**Generated**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("**Total Failures**: ${failedResponses.size}\n\n")
            writer.write("---\n\n")

            failedResponses.forEachIndexed { index, response ->
                writer.write("## Failure ${index + 1}\n\n")
                writer.write("**Timestamp**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(response.requestedAt))}\n")
                writer.write("**Response Time**: ${response.responseTimeMs}ms\n")
                writer.write("**Model**: ${response.modelName}\n")
                writer.write("**Error**: ${response.errorMessage ?: "Unknown error"}\n\n")

                writer.write("### User Prompt\n")
                writer.write("```\n${response.userPrompt}\n```\n\n")

                writer.write("### Raw Response\n")
                writer.write("```json\n${formatJson(response.rawResponse)}\n```\n\n")

                writer.write("---\n\n")
            }
        }

        return outputFile
    }

    /**
     * Pretty-print JSON for better readability.
     * If JSON is malformed, returns as-is.
     */
    private fun formatJson(jsonString: String): String {
        return try {
            val json = Json { prettyPrint = true }
            val element = Json.parseToJsonElement(jsonString)
            json.encodeToString(JsonElement.serializer(), element)
        } catch (e: Exception) {
            // If JSON is malformed, return as-is
            jsonString
        }
    }

    /**
     * Get flow of recent responses for UI display.
     */
    fun getRecentResponses(limit: Int = 50) = llmResponseDao.getRecentResponses(limit)

    /**
     * Get flow of failed parses for debugging UI.
     */
    fun getFailedParses() = llmResponseDao.getFailedParses()

    /**
     * Clean up old responses to prevent unbounded storage growth.
     *
     * @param daysToKeep Number of days to retain (default 30)
     * @return Number of responses deleted
     */
    suspend fun cleanupOldResponses(daysToKeep: Int = 30): Int {
        val cutoffMillis = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        return llmResponseDao.deleteResponsesBefore(cutoffMillis)
    }

    /**
     * Get statistics about stored responses.
     */
    suspend fun getStatistics(): ResponseStatistics {
        val totalCount = llmResponseDao.getResponseCount()
        val failedCount = llmResponseDao.getFailedParseCount()
        val successRate = if (totalCount > 0) {
            ((totalCount - failedCount).toDouble() / totalCount * 100).toInt()
        } else {
            0
        }

        return ResponseStatistics(
            totalResponses = totalCount,
            failedParses = failedCount,
            successRate = successRate
        )
    }

    data class ResponseStatistics(
        val totalResponses: Int,
        val failedParses: Int,
        val successRate: Int
    )
}
