package ru.llm.agent.usecase.rag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.RagSourceRepository

/**
 * Use case для получения сообщений диалога с привязанными источниками RAG.
 * Комбинирует данные сообщений с данными источников для отображения в UI.
 */
public class GetMessagesWithRagSourcesUseCase(
    private val conversationRepository: ConversationRepository,
    private val ragSourceRepository: RagSourceRepository
) {
    /**
     * Получить сообщения диалога с источниками RAG
     *
     * @param conversationId ID диалога
     * @return Flow со списком сообщений, где каждое сообщение ассистента
     *         содержит список использованных источников
     */
    public suspend operator fun invoke(conversationId: String): Flow<List<ConversationMessage>> {
        return conversationRepository.getMessages(conversationId).combine(
            ragSourceRepository.getSourcesForConversation(conversationId)
        ) { messages, allSources ->
            // Группируем источники по ID сообщения
            val sourcesByMessageId = allSources.groupBy { it.messageId }

            // Добавляем источники к сообщениям ассистента
            messages.map { message ->
                if (message.role == Role.ASSISTANT) {
                    val sources = sourcesByMessageId[message.id] ?: emptyList()
                    message.copy(ragSources = sources)
                } else {
                    message
                }
            }
        }
    }
}
