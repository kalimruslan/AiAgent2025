package ru.llm.agent.model.conversation

import ru.llm.agent.model.ExpertOpinion
import ru.llm.agent.model.Role

public data class ConversationMessage(
    val id: Long = 0,
    val conversationId: String,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isContinue: Boolean = false,
    val isComplete: Boolean = false,
    val originalResponse: String? = null,
    val model: String,
    /** Мнения экспертов, связанные с этим сообщением (для режима Committee) */
    val expertOpinions: List<ExpertOpinion> = emptyList(),
    /** Количество входных токенов (из запроса) */
    val inputTokens: Int? = null,
    /** Количество токенов в ответе (completion tokens) */
    val completionTokens: Int? = null,
    /** Общее количество токенов */
    val totalTokens: Int? = null,
    /** Время ответа LLM в миллисекундах */
    val responseTimeMs: Long? = null,
    /** Флаг суммаризированного сообщения (история диалога была сжата в это сообщение) */
    val isSummarized: Boolean = false
)