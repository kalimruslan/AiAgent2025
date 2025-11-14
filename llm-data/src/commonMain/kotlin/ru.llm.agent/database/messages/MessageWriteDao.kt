package ru.llm.agent.database.messages

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * DAO для операций записи сообщений.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface MessageWriteDao {
    /**
     * Вставить новое сообщение
     * @return ID вставленного сообщения
     */
    @Insert
    public suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Обновить существующее сообщение
     * @return количество обновлённых строк
     */
    @Update
    public suspend fun updateMessage(message: MessageEntity): Int

    /**
     * Очистить диалог (удалить все сообщения кроме системных)
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND role != 'system'")
    public suspend fun clearConversation(conversationId: String)

    /**
     * Удалить весь диалог (включая системные сообщения)
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    public suspend fun deleteConversation(conversationId: String)

    /**
     * Удалить все сообщения из БД
     */
    @Query("DELETE FROM messages")
    public suspend fun deleteAll()

    /**
     * Удалить сообщения по списку ID
     */
    @Query("DELETE FROM messages WHERE id IN (:messageIds)")
    public suspend fun deleteMessagesByIds(messageIds: List<Long>)
}