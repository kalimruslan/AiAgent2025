package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.ExpertOpinion

/**
 * Репозиторий для работы с мнениями экспертов
 */
public interface ExpertRepository {

    /**
     * Сохранить мнение эксперта
     */
    public suspend fun saveExpertOpinion(
        expertId: String,
        expertName: String,
        expertIcon: String,
        messageId: Long,
        conversationId: String,
        opinion: String,
        timestamp: Long,
        originalResponse: String?
    ): Long

    /**
     * Получить все мнения для конкретного сообщения
     */
    public fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinion>>

    /**
     * Получить все мнения для диалога
     */
    public fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinion>>

    /**
     * Удалить все мнения для диалога
     */
    public suspend fun deleteOpinionsForConversation(conversationId: String)

    /**
     * Удалить все мнения для конкретного сообщения
     */
    public suspend fun deleteOpinionsForMessage(messageId: Long)

    /**
     * Получить количество мнений для сообщения
     */
    public suspend fun getOpinionsCountForMessage(messageId: Long): Int
}
