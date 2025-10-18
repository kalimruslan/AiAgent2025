package ru.llm.agent.model.conversation

public data class ConversationState (
    val isComplete: Boolean = false,
    val finalResult: String? = null
)