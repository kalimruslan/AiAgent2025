package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository

/**
 *  Юзкейс для работы с диалогами
 */
public class ConversationUseCase(
    private val repository: ConversationRepository
){
    public suspend operator fun invoke(
        conversationId: String
    ): Flow<List<ConversationMessage>> {
        repository.initializeConversation(conversationId)

        return repository.getMessages(conversationId)
    }

    public suspend fun clearConversation(conversationId: String){
        repository.clearConversation(conversationId, initNew = true)
    }
}