package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Репозиторий для работы с сообщениями в диалогах.
 * Отвечает за CRUD операции с сообщениями и базовую инициализацию диалогов.
 */
public interface ConversationRepository {
    /**
     * Инициализировать новый диалог с системным сообщением
     */
    public suspend fun initializeConversation(conversationId: String)

    /**
     * Получить все сообщения диалога
     */
    public suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>>

    /**
     * Получить сообщения вместе с мнениями экспертов (для режима Committee)
     */
    public suspend fun getMessagesWithExpertOpinions(conversationId: String): Flow<List<ConversationMessage>>

    /**
     * Сохранить сообщение пользователя в БД
     *
     * @return ID созданного сообщения
     */
    public suspend fun saveUserMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider
    ): Long

    /**
     * Сохранить сообщение ассистента в БД
     *
     * @return ID созданного сообщения
     */
    public suspend fun saveAssistantMessage(
        conversationMessage: ConversationMessage
    ): Long

    /**
     * Получить все сообщения диалога синхронно (для use cases)
     */
    public suspend fun getMessagesByConversationSync(conversationId: String): List<ConversationMessage>

    /**
     * Очистить диалог
     */
    public suspend fun clearConversation(conversationId: String, initNew: Boolean)

    /**
     * Удалить диалог
     */
    public suspend fun deleteConversation(conversationId: String, initNew: Boolean)

    /**
     * Получить список всех диалогов
     */
    public fun getAllConversations(): Flow<List<String>>

    /**
     * Получить контекст диалога (температура, system prompt, maxTokens)
     */
    public suspend fun getContext(conversationId: String): Flow<ru.llm.agent.model.ConversationContext?>

    /**
     * Получить информацию о суммаризации истории диалога
     *
     * @param conversationId ID диалога
     * @return Flow с информацией о суммаризации
     */
    public suspend fun getSummarizationInfo(conversationId: String): Flow<ru.llm.agent.model.SummarizationInfo>

    /**
     * Удалить сообщения по их ID
     *
     * @param messageIds Список ID сообщений для удаления
     */
    public suspend fun deleteMessages(messageIds: List<Long>)

    /**
     * Сохранить системное сообщение (например, суммаризацию)
     *
     * @param conversationId ID диалога
     * @param text Текст сообщения
     * @param isSummarized Флаг суммаризированного сообщения
     * @param totalTokens Количество токенов в сообщении (для отслеживания использования)
     * @return ID созданного сообщения
     */
    public suspend fun saveSystemMessage(
        conversationId: String,
        text: String,
        isSummarized: Boolean = false,
        totalTokens: Int? = null
    ): Long
}