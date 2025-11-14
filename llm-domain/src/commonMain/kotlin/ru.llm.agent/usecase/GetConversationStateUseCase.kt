package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.model.conversation.Message
import ru.llm.agent.repository.MessageRepository
import ru.llm.agent.repository.SummarizationRepository
import ru.llm.agent.repository.TokenManagementRepository
import ru.llm.agent.repository.TokenUsage
import ru.llm.agent.usecase.base.FlowUseCase

/**
 * Use Case для получения полного состояния диалога.
 * Объединяет данные из нескольких источников: сообщения, токены, суммаризацию.
 *
 * Следует принципу Single Responsibility и Clean Architecture.
 */
public class GetConversationStateUseCase(
    private val messageRepository: MessageRepository,
    private val tokenManagementRepository: TokenManagementRepository,
    private val summarizationRepository: SummarizationRepository
) {

    public suspend operator fun invoke(input: ConversationId): Flow<ConversationStateData> {
        // Получаем сообщения
        val messagesFlow = messageRepository.getMessages(input)

        // Получаем использование токенов
        val tokenUsageFlow = tokenManagementRepository.getTokenUsage(input)

        // Получаем информацию о суммаризации
        val summarizationFlow = summarizationRepository.getSummarizationInfo(input)

        // Объединяем все потоки данных
        return combine(
            messagesFlow,
            tokenUsageFlow,
            summarizationFlow
        ) { messages, tokenUsage, summarization ->
            ConversationStateData(
                messages = messages,
                tokenUsage = tokenUsage,
                summarizationInfo = summarization
            )
        }
    }
}

/**
 * Данные о состоянии диалога
 */
public data class ConversationStateData(
    /** Список сообщений диалога */
    val messages: List<Message>,
    /** Информация об использовании токенов */
    val tokenUsage: TokenUsage,
    /** Информация о суммаризации */
    val summarizationInfo: SummarizationInfo
)