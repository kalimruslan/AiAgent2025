package ru.llm.agent.database.context

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций чтения контекста диалога.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface ContextReadDao {
    /**
     * Получить контекст диалога (синхронно)
     */
    @Query("SELECT * FROM context WHERE conversationId = :conversationId")
    public suspend fun getContextByConversationId(conversationId: String): ContextEntity?

    /**
     * Получить контекст диалога (Flow)
     */
    @Query("SELECT * FROM context WHERE conversationId = :conversationId")
    public fun getContextByConversationIdFlow(conversationId: String): Flow<ContextEntity?>

    /**
     * Получить все контексты (для отладки или экспорта)
     */
    @Query("SELECT * FROM context ORDER BY timestamp DESC")
    public fun getAllContexts(): Flow<List<ContextEntity>>

    /**
     * Получить контекст по ID
     */
    @Query("SELECT * FROM context WHERE id = :id")
    public suspend fun getContextById(id: Long): ContextEntity?
}