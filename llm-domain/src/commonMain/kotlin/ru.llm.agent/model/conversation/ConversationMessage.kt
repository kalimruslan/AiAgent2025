package ru.llm.agent.model.conversation

import ru.llm.agent.model.Role

public data class ConversationMessage(
    val id: Long = 0,
    val conversationId: String,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isContinue: Boolean,
    val isComplete: Boolean,
    val originalResponse: String? = null,
    val model: String
)