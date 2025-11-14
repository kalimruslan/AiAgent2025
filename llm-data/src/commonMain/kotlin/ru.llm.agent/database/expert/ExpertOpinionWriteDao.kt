package ru.llm.agent.database.expert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO для операций записи мнений экспертов.
 * Разделение на read/write DAO улучшает тестируемость и следует Single Responsibility Principle.
 */
@Dao
public interface ExpertOpinionWriteDao {
    /**
     * Вставить мнение эксперта
     * @return ID вставленного мнения
     */
    @Insert
    public suspend fun insertOpinion(opinion: ExpertOpinionEntity): Long

    /**
     * Удалить все мнения для диалога
     */
    @Query("DELETE FROM expert_opinions WHERE conversationId = :conversationId")
    public suspend fun deleteOpinionsForConversation(conversationId: String)

    /**
     * Удалить все мнения для конкретного сообщения
     */
    @Query("DELETE FROM expert_opinions WHERE messageId = :messageId")
    public suspend fun deleteOpinionsForMessage(messageId: Long)

    /**
     * Удалить все мнения
     */
    @Query("DELETE FROM expert_opinions")
    public suspend fun deleteAllOpinions()
}