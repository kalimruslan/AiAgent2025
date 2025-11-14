package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.model.SummarizationInfo

/**
 * Репозиторий для управления суммаризацией истории диалогов.
 * Отвечает за сохранение и получение информации о суммаризированных сообщениях.
 */
public interface SummarizationRepository {
    /**
     * Получить информацию о суммаризации для диалога
     */
    public suspend fun getSummarizationInfo(conversationId: ConversationId): Flow<SummarizationInfo>

    /**
     * Сохранить суммаризированное сообщение
     *
     * @param conversationId ID диалога
     * @param summarizedText Текст суммаризации
     * @param originalMessageIds ID исходных сообщений, которые были суммаризированы
     * @param tokensCount Количество токенов в суммаризации
     * @return ID созданного суммаризированного сообщения
     */
    public suspend fun saveSummarization(
        conversationId: ConversationId,
        summarizedText: String,
        originalMessageIds: List<MessageId>,
        tokensCount: TokenCount
    ): MessageId

    /**
     * Получить все суммаризированные сообщения диалога
     */
    public suspend fun getSummarizedMessages(conversationId: ConversationId): List<SummarizedMessage>

    /**
     * Проверить, было ли сообщение суммаризировано
     */
    public suspend fun isSummarized(messageId: MessageId): Boolean

    /**
     * Получить оригинальные сообщения для суммаризированного
     */
    public suspend fun getOriginalMessages(summarizedMessageId: MessageId): List<MessageId>

    /**
     * Удалить информацию о суммаризации
     */
    public suspend fun deleteSummarization(summarizedMessageId: MessageId)
}

/**
 * Данные о суммаризированном сообщении
 */
public data class SummarizedMessage(
    /** ID суммаризированного сообщения */
    val id: MessageId,
    /** ID диалога */
    val conversationId: ConversationId,
    /** Текст суммаризации */
    val text: String,
    /** ID исходных сообщений */
    val originalMessageIds: List<MessageId>,
    /** Количество суммаризированных сообщений */
    val originalMessageCount: Int,
    /** Количество токенов */
    val tokens: TokenCount,
    /** Время создания */
    val timestamp: Long
)