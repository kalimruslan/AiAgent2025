package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.rag.RagSource

/**
 * Репозиторий для работы с источниками RAG.
 * Управляет сохранением и загрузкой источников, использованных при генерации ответов.
 */
public interface RagSourceRepository {
    /**
     * Сохранить источники для сообщения
     *
     * @param messageId ID сообщения ассистента
     * @param conversationId ID диалога
     * @param sources Список источников для сохранения
     */
    public suspend fun saveSources(
        messageId: Long,
        conversationId: String,
        sources: List<RagSource>
    )

    /**
     * Получить все источники для диалога (реактивно)
     */
    public fun getSourcesForConversation(conversationId: String): Flow<List<RagSource>>

    /**
     * Получить все источники для диалога синхронно
     */
    public suspend fun getSourcesForConversationSync(conversationId: String): List<RagSource>

    /**
     * Получить источники для конкретного сообщения
     */
    public suspend fun getSourcesForMessage(messageId: Long): List<RagSource>

    /**
     * Удалить источники для диалога
     */
    public suspend fun deleteSourcesForConversation(conversationId: String)

    /**
     * Удалить источники для конкретного сообщения
     */
    public suspend fun deleteSourcesForMessage(messageId: Long)
}
