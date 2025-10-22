package ru.llm.agent.usecase.context

import ru.llm.agent.model.ConversationContext
import ru.llm.agent.repository.LocalDbRepository

public class RemoveLocalContextUseCase(
    public val repository: LocalDbRepository
) {
    public suspend operator fun invoke(conversationId: String) {
        repository.removeContextByConversation(conversationId)
    }
}