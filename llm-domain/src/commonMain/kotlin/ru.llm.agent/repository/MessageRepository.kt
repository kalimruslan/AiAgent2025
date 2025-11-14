package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.model.conversation.Message

/**
 * Репозиторий для CRUD операций с сообщениями.
 * Фокусируется исключительно на работе с индивидуальными сообщениями.
 */
public interface MessageRepository {
    /**
     * Получить сообщение по ID
     */
    public suspend fun getMessage(id: MessageId): Message?

    /**
     * Получить все сообщения диалога
     */
    public suspend fun getMessages(conversationId: ConversationId): Flow<List<Message>>

    /**
     * Получить сообщения диалога синхронно
     */
    public suspend fun getMessagesSync(conversationId: ConversationId): List<Message>

    /**
     * Сохранить сообщение пользователя
     * @return ID созданного сообщения
     */
    public suspend fun saveUserMessage(message: Message.User): MessageId

    /**
     * Сохранить сообщение ассистента
     * @return ID созданного сообщения
     */
    public suspend fun saveAssistantMessage(message: Message.Assistant): MessageId

    /**
     * Сохранить системное сообщение
     * @return ID созданного сообщения
     */
    public suspend fun saveSystemMessage(message: Message.System): MessageId

    /**
     * Обновить существующее сообщение
     */
    public suspend fun updateMessage(message: Message)

    /**
     * Удалить сообщение по ID
     */
    public suspend fun deleteMessage(id: MessageId)

    /**
     * Удалить несколько сообщений
     */
    public suspend fun deleteMessages(ids: List<MessageId>)

    /**
     * Удалить все сообщения диалога
     */
    public suspend fun deleteAllMessages(conversationId: ConversationId)

    /**
     * Получить количество сообщений в диалоге
     */
    public suspend fun getMessageCount(conversationId: ConversationId): Int

    /**
     * Получить последние N сообщений диалога
     */
    public suspend fun getLastMessages(conversationId: ConversationId, count: Int): List<Message>
}