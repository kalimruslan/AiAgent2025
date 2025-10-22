package ru.llm.agent.repository

import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.model.ConversationContext
import ru.llm.agent.toEntity
import ru.llm.agent.toModel

public class LocalDbRepositoryImpl(
    private val contextDao: ContextDao
): LocalDbRepository {

    override suspend fun saveContextByConversation(conversationId: String, conversationContext: ConversationContext){
        contextDao.upsertContext(conversationContext.toEntity(conversationId))
    }

    override suspend fun getContextByConversation(conversationId: String): ConversationContext? {
        return contextDao.getContextByConversationId(conversationId)?.toModel()
    }

    override suspend fun removeContextByConversation(conversationId: String) {
        contextDao.deleteContextByConversationId(conversationId)
    }

    override suspend fun deleteAllContexts() {
        contextDao.deleteAllContexts()
    }
}