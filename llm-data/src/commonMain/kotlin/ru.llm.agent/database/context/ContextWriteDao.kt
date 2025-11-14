package ru.llm.agent.database.context

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * DAO для операций записи контекста диалога.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface ContextWriteDao {
    /**
     * Вставить новый контекст
     * @return ID вставленного контекста
     */
    @Insert
    public suspend fun insertContext(context: ContextEntity): Long

    /**
     * Обновить существующий контекст
     * @return количество обновлённых строк
     */
    @Update
    public suspend fun updateContext(context: ContextEntity): Int

    /**
     * Удалить контекст по conversationId
     */
    @Query("DELETE FROM context WHERE conversationId = :conversationId")
    public suspend fun deleteContextByConversationId(conversationId: String)

    /**
     * Удалить все контексты
     */
    @Query("DELETE FROM context")
    public suspend fun deleteAllContexts()
}