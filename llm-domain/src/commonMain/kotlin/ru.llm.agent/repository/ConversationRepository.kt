package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.conversation.ConversationMessage

public interface ConversationRepository {
    public suspend fun initializeConversation(conversationId: String)

    public suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>>

    public suspend fun sendMessage(
        conversationId: String,
        message: String,
        model: String
    ): Flow<NetworkResult<ConversationMessage>>

    public suspend fun clearConversation(conversationId: String, initNew: Boolean)

    public suspend fun deleteConversation(conversationId: String, initNew: Boolean)

    public fun getAllConversations(): Flow<List<String>>
}