package ru.llm.agent.repository

import ru.llm.agent.model.ConversationContext

public interface LocalDbRepository{
    public suspend fun saveContextByConversation(conversationId: String, conversationContext: ConversationContext)
    public suspend fun getContextByConversation(conversationId: String): ConversationContext?

    public suspend fun removeContextByConversation(conversationId: String)
    public suspend fun deleteAllContexts()
}