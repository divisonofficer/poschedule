package com.jnkim.poschedule.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenAiChatRequest(
    val model: String = "m1", // as per plan example
    val messages: List<ChatMessage>,
    val site_name: String
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class GenAiChatResponse(
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val message: ChatMessage
)

@Serializable
data class GentleCopyResponse(
    val title: String,
    val body: String,
    val toneTag: String? = null,
    val safetyTag: String? = null
)
