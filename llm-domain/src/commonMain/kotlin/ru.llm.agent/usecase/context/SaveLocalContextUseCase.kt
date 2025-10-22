package ru.llm.agent.usecase.context

import ru.llm.agent.model.ConversationContext
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LocalDbRepository

public class SaveLocalContextUseCase(
    private val repository: LocalDbRepository,
    private val conversationRepository: ConversationRepository
) {
    public suspend operator fun invoke(conversationId: String, context: ConversationContext) {
        repository.saveContextByConversation(conversationId, context)
        //conversationRepository.deleteConversation(conversationId)
    }
}