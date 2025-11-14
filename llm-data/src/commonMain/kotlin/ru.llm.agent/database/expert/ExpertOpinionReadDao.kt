package ru.llm.agent.database.expert

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций чтения мнений экспертов.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface ExpertOpinionReadDao {
    /**
     * Получить все мнения для конкретного сообщения (Flow)
     */
    @Query("SELECT * FROM expert_opinions WHERE messageId = :messageId ORDER BY timestamp ASC")
    public fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinionEntity>>

    /**
     * Получить все мнения для диалога (Flow)
     */
    @Query("SELECT * FROM expert_opinions WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinionEntity>>

    /**
     * Получить все мнения для диалога (синхронно)
     */
    @Query("SELECT * FROM expert_opinions WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public suspend fun getOpinionsForConversationSync(conversationId: String): List<ExpertOpinionEntity>

    /**
     * Получить количество мнений для сообщения
     */
    @Query("SELECT COUNT(*) FROM expert_opinions WHERE messageId = :messageId")
    public suspend fun getOpinionsCountForMessage(messageId: Long): Int

    /**
     * Получить мнение по ID
     */
    @Query("SELECT * FROM expert_opinions WHERE id = :id")
    public suspend fun getOpinionById(id: Long): ExpertOpinionEntity?
}