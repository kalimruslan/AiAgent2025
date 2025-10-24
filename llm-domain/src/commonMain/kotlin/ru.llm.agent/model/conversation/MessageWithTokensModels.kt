package ru.llm.agent.model.conversation

public data class MessageWithTokensModels(
    val role: String,
    val text: String,
    var tokens: Int = 0
)