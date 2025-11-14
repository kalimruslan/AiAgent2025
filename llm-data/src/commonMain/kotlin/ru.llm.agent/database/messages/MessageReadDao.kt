package ru.llm.agent.database.messages

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций чтения сообщений.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface MessageReadDao {
    /**
     * Получить все сообщения для диалога (Flow)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Получить все сообщения для диалога (синхронно)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public suspend fun getMessagesByConversationSync(conversationId: String): List<MessageEntity>

    /**
     * Получить все уникальные диалоги
     */
    @Query("SELECT DISTINCT conversationId FROM messages ORDER BY timestamp DESC")
    public fun getAllConversations(): Flow<List<String>>

    /**
     * Получить системное сообщение для диалога
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND role = 'system' LIMIT 1")
    public suspend fun getSystemMessageByConversation(conversationId: String): MessageEntity?

    /**
     * Получить количество суммаризированных сообщений в диалоге
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND isSummarized = 1")
    public suspend fun getSummarizedMessagesCount(conversationId: String): Int

    /**
     * Получить все суммаризированные сообщения в диалоге
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isSummarized = 1 ORDER BY timestamp DESC")
    public suspend fun getSummarizedMessages(conversationId: String): List<MessageEntity>

    /**
     * Получить сообщение по ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    public suspend fun getMessageById(messageId: Long): MessageEntity?

    /**
     * Получить несколько сообщений по списку ID
     */
    @Query("SELECT * FROM messages WHERE id IN (:messageIds) ORDER BY timestamp ASC")
    public suspend fun getMessagesByIds(messageIds: List<Long>): List<MessageEntity>
}