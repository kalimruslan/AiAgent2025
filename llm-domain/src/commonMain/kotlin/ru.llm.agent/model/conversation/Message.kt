package ru.llm.agent.model.conversation

import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.model.ExpertOpinion

/**
 * Sealed interface для представления сообщений в диалоге.
 * Обеспечивает типобезопасность и явное разделение типов сообщений.
 */
public sealed interface Message {
    /** Уникальный идентификатор сообщения */
    public val id: MessageId

    /** Идентификатор диалога, к которому относится сообщение */
    public val conversationId: ConversationId

    /** Текст сообщения */
    public val text: String

    /** Временная метка создания сообщения */
    public val timestamp: Long

    /** Модель LLM, использованная для генерации (для ассистента) */
    public val model: String?

    /** Количество входных токенов */
    public val inputTokens: TokenCount?

    /** Количество токенов в ответе */
    public val completionTokens: TokenCount?

    /** Общее количество токенов */
    public val totalTokens: TokenCount?

    /** Время ответа LLM в миллисекундах */
    public val responseTimeMs: Long?

    /** Флаг суммаризированного сообщения */
    public val isSummarized: Boolean

    /**
     * Сообщение от пользователя
     */
    public data class User(
        override val id: MessageId,
        override val conversationId: ConversationId,
        override val text: String,
        override val timestamp: Long = 0L,
        override val inputTokens: TokenCount? = null,
        override val totalTokens: TokenCount? = null,
        override val isSummarized: Boolean = false
    ) : Message {
        override val model: String? = null
        override val completionTokens: TokenCount? = null
        override val responseTimeMs: Long? = null
    }

    /**
     * Сообщение от ассистента
     */
    public data class Assistant(
        override val id: MessageId,
        override val conversationId: ConversationId,
        override val text: String,
        override val model: String,
        override val timestamp: Long = 0L,
        /** Флаг продолжения предыдущего ответа */
        val isContinue: Boolean = false,
        /** Флаг завершённости ответа */
        val isComplete: Boolean = false,
        /** Оригинальный ответ от LLM (до парсинга) */
        val originalResponse: String? = null,
        /** Мнения экспертов (для режима Committee) */
        val expertOpinions: List<ExpertOpinion> = emptyList(),
        override val inputTokens: TokenCount? = null,
        override val completionTokens: TokenCount? = null,
        override val totalTokens: TokenCount? = null,
        override val responseTimeMs: Long? = null,
        override val isSummarized: Boolean = false
    ) : Message

    /**
     * Системное сообщение (промпт)
     */
    public data class System(
        override val id: MessageId,
        override val conversationId: ConversationId,
        override val text: String,
        override val timestamp: Long = 0L,
        override val isSummarized: Boolean = false
    ) : Message {
        override val model: String? = null
        override val inputTokens: TokenCount? = null
        override val completionTokens: TokenCount? = null
        override val totalTokens: TokenCount? = null
        override val responseTimeMs: Long? = null
    }
}

/**
 * Extension функция для получения роли сообщения
 */
public val Message.role: String
    get() = when (this) {
        is Message.User -> "user"
        is Message.Assistant -> "assistant"
        is Message.System -> "system"
    }