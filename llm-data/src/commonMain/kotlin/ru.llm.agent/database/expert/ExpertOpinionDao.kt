package ru.llm.agent.database.expert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public interface ExpertOpinionDao {

    /**
     * Вставить мнение эксперта
     */
    @Insert
    public suspend fun insertOpinion(opinion: ExpertOpinionEntity): Long

    /**
     * Получить все мнения для конкретного сообщения
     */
    @Query("SELECT * FROM expert_opinions WHERE messageId = :messageId ORDER BY timestamp ASC")
    public fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinionEntity>>

    /**
     * Получить все мнения для диалога
     */
    @Query("SELECT * FROM expert_opinions WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinionEntity>>

    /**
     * Получить все мнения для диалога (синхронно)
     */
    @Query("SELECT * FROM expert_opinions WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public suspend fun getOpinionsForConversationSync(conversationId: String): List<ExpertOpinionEntity>

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
     * Получить количество мнений для сообщения
     */
    @Query("SELECT COUNT(*) FROM expert_opinions WHERE messageId = :messageId")
    public suspend fun getOpinionsCountForMessage(messageId: Long): Int
}
