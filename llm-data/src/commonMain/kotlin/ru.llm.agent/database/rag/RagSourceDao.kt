package ru.llm.agent.database.rag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с таблицей rag_sources.
 * Управляет источниками RAG, связанными с сообщениями.
 */
@Dao
public interface RagSourceDao {
    /**
     * Вставить список источников
     */
    @Insert
    public suspend fun insertAll(sources: List<RagSourceEntity>)

    /**
     * Получить источники для конкретного сообщения
     */
    @Query("SELECT * FROM rag_sources WHERE messageId = :messageId ORDER BY `index`")
    public suspend fun getSourcesByMessageId(messageId: Long): List<RagSourceEntity>

    /**
     * Получить все источники для диалога (Flow для реактивного обновления)
     */
    @Query("SELECT * FROM rag_sources WHERE conversationId = :conversationId ORDER BY timestamp DESC, `index`")
    public fun getSourcesForConversation(conversationId: String): Flow<List<RagSourceEntity>>

    /**
     * Получить все источники для диалога синхронно
     */
    @Query("SELECT * FROM rag_sources WHERE conversationId = :conversationId ORDER BY timestamp DESC, `index`")
    public suspend fun getSourcesForConversationSync(conversationId: String): List<RagSourceEntity>

    /**
     * Удалить источники для конкретного сообщения
     */
    @Query("DELETE FROM rag_sources WHERE messageId = :messageId")
    public suspend fun deleteSourcesForMessage(messageId: Long)

    /**
     * Удалить все источники для диалога
     */
    @Query("DELETE FROM rag_sources WHERE conversationId = :conversationId")
    public suspend fun deleteSourcesForConversation(conversationId: String)

    /**
     * Получить количество источников в диалоге
     */
    @Query("SELECT COUNT(*) FROM rag_sources WHERE conversationId = :conversationId")
    public suspend fun getSourcesCount(conversationId: String): Int
}
