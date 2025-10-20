package ru.llm.agent.model

public data class Settings(
    val temperature: Double,
    val systemPrompt: String,
    val maxTokens: Int,
    val timestamp: Long
)