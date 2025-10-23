package ru.llm.agent.database.context

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
public interface ContextDao {

    @Query("SELECT * FROM context WHERE conversationId = :conversationId")
    public suspend fun getContextByConversationId(conversationId: String): ContextEntity?

    @Insert
    public suspend fun insertSettings(message: ContextEntity): Long

    @Update
    public suspend fun updateSettings(message: ContextEntity): Int

    @Query("DELETE FROM context WHERE conversationId = :conversationId")
    public suspend fun deleteContextByConversationId(conversationId: String)

    @Query("DELETE FROM context")
    public suspend fun deleteAllContexts()

    public suspend fun upsertContext(context: ContextEntity) {
        val existing = getContextByConversationId(context.conversationId)
        if (existing != null) {
            // Обновляем
            updateSettings(context.copy(id = existing.id))
        } else {
            // Вставляем новую
            insertSettings(context)
        }
    }
}