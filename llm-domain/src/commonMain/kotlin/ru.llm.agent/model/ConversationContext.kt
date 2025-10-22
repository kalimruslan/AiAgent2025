package ru.llm.agent.model

public data class ConversationContext(
    val temperature: Double,
    val systemPrompt: String,
    val maxTokens: Int,
    val timestamp: Long
)