package ru.llm.agent.usecase

import ru.llm.agent.model.Settings
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LocalDbRepository

public class SendOptionsToLocalDbUseCase(
    private val repository: LocalDbRepository,
    private val conversationRepository: ConversationRepository
) {
    public suspend operator fun invoke(settings: Settings) {
        repository.saveSettings(settings)
        conversationRepository.deleteConversation("default_conversation")
    }
}