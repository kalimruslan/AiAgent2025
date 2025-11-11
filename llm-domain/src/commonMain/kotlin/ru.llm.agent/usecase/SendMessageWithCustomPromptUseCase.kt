package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.service.MessageSendingService

/**
 * Use case для отправки сообщения с кастомным системным промптом.
 * Используется для экспертов, синтеза и других специальных кейсов.
 *
 * Логика:
 * 1. Формирует список сообщений: system prompt + user message
 * 2. Отправляет через MessageSendingService
 * 3. Возвращает результат (НЕ сохраняет в БД автоматически)
 */
public class SendMessageWithCustomPromptUseCase(
    private val messageSendingService: MessageSendingService,
    private val providerConfigRepository: ProviderConfigRepository,
) {
    /**
     * Отправить сообщение с кастомным системным промптом
     *
     * @param conversationId ID диалога (может быть временным для экспертов)
     * @param userMessage Сообщение пользователя
     * @param systemPrompt Кастомный системный промпт
     * @param provider LLM провайдер
     * @return Flow с результатом отправки
     */
    public suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        systemPrompt: String,
        provider: LlmProvider
    ): Flow<NetworkResult<ConversationMessage>> {
        // Формируем список сообщений с правильными ролями
        val messages = listOf(
            ConversationMessage(
                id = 0L,
                conversationId = conversationId,
                role = Role.SYSTEM,
                text = systemPrompt,
                timestamp = System.currentTimeMillis(),
                model = provider.displayName
            ),
            ConversationMessage(
                id = 0L,
                conversationId = conversationId,
                role = Role.USER,
                text = userMessage,
                timestamp = System.currentTimeMillis(),
                model = provider.displayName
            )
        )

        // Отправляем через сервис
        return messageSendingService.sendMessages(
            conversationId = conversationId,
            messages = messages,
            provider = provider,
            temperature = null, // Для кастомных промптов используем значения по умолчанию
            maxTokens = null
        ).map { networkResult ->
            when (networkResult) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(networkResult.data.conversationMessage)
                }
                is NetworkResult.Error -> {
                    NetworkResult.Error(networkResult.error)
                }
                is NetworkResult.Loading -> {
                    NetworkResult.Loading()
                }
            }
        }
    }
}