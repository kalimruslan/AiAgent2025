package ru.llm.agent.model

public data class MessageModel(
    val role: String,
    val content: String,
    val refusal: String? = null,
    val reasoning: String = "",
    val format: String = "",
    val parsedFormats: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L,
    val tokenUsed: Int = 0
)