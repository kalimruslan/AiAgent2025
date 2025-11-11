package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository

/**
 * Use case для получения сообщений диалога вместе с мнениями экспертов.
 * Используется в режиме Committee для отображения мнений экспертов под каждым сообщением пользователя.
 */
public class GetMessagesWithExpertOpinionsUseCase(
    private val conversationRepository: ConversationRepository
) {
    /**
     * Получить сообщения с мнениями экспертов
     *
     * @param conversationId ID диалога
     * @return Flow со списком сообщений, где к каждому USER сообщению прикреплены мнения экспертов
     */
    public suspend operator fun invoke(conversationId: String): Flow<List<ConversationMessage>> {
        return conversationRepository.getMessagesWithExpertOpinions(conversationId)
    }
}