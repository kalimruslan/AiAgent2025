package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage

public interface ConversationRepository {
    public suspend fun initializeConversation(conversationId: String)

    public suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>>

    /** Получить сообщения вместе с мнениями экспертов (для режима Committee) */
    public suspend fun getMessagesWithExpertOpinions(conversationId: String): Flow<List<ConversationMessage>>

    public suspend fun sendMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider
    ): Flow<NetworkResult<ConversationMessage>>

    public suspend fun clearConversation(conversationId: String, initNew: Boolean)

    public suspend fun deleteConversation(conversationId: String, initNew: Boolean)

    public fun getAllConversations(): Flow<List<String>>

    /** Получить выбранный провайдер для диалога */
    public suspend fun getSelectedProvider(conversationId: String): LlmProvider

    /** Сохранить выбранный провайдер для диалога */
    public suspend fun saveSelectedProvider(conversationId: String, provider: LlmProvider)
}