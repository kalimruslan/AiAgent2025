package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository

/**
 * Юзкейс для отправки сообщения в диалог
 */
public class SendConversationMessageUseCase(
    private val repository: ConversationRepository
){
    public suspend operator fun invoke(
        conversationId: String,
        message: String,
        provider: LlmProvider
    ): Flow<NetworkResult<ConversationMessage>> {
        return repository.sendMessage(
            conversationId = conversationId,
            message = message,
            provider = provider
        )
    }
}