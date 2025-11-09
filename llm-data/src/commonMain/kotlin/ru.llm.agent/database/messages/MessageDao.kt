package ru.llm.agent.database.messages

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.llm.agent.database.context.ContextEntity

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public suspend fun getMessagesByConversationSync(conversationId: String): List<MessageEntity>

    @Insert
    public suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND role != 'system'")
    public suspend fun clearConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    public suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM messages")
    public suspend fun deleteAll()

    @Query("SELECT DISTINCT conversationId FROM messages ORDER BY timestamp DESC")
    public fun getAllConversations(): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND role = 'system' LIMIT 1")
    public suspend fun getSystemMessageByConversation(conversationId: String): MessageEntity?

    @Update
    public suspend fun updateMessage(message: MessageEntity): Int

    public suspend fun upsertSystemMessage(message: MessageEntity) {
        val existing = getSystemMessageByConversation(message.conversationId)
        if (existing != null) {
            // Обновляем
            updateMessage(message.copy(id = existing.id))
        } else {
            // Вставляем новую
            insertMessage(message)
        }
    }
}