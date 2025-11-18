package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.service.MessageSendingService

/**
 * Use case для отправки сообщения в диалог.
 *
 * Логика:
 * 1. Сохраняет сообщение пользователя в БД
 * 2. Получает всю историю диалога
 * 3. Отправляет через MessageSendingService
 * 4. Сохраняет ответ ассистента в БД
 * 5. Возвращает сообщение ассистента
 */
public class SendConversationMessageUseCase(
    private val conversationRepository: ConversationRepository,
    private val messageSendingService: MessageSendingService
) {
    public suspend operator fun invoke(
        conversationId: String,
        message: String,
        provider: LlmProvider,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Flow<NetworkResult<ConversationMessage>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Валидация: не отправляем пустые сообщения
            if (message.isBlank()) {
                emit(
                    NetworkResult.Error(
                        DomainError.ValidationError(
                            field = "message",
                            message = "Сообщение не может быть пустым"
                        )
                    )
                )
                return@flow
            }

            // 1. Сохраняем сообщение пользователя
            conversationRepository.saveUserMessage(conversationId, message, provider)

            // 2. Получаем всю историю диалога
            val allMessages = conversationRepository.getMessagesByConversationSync(conversationId)

            // 3. Отправляем через сервис
            messageSendingService.sendMessages(
                conversationId = conversationId,
                messages = allMessages,
                provider = provider,
                temperature = temperature,
                maxTokens = maxTokens
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // 4. Сохраняем ответ ассистента в БД
                        val assistantId = conversationRepository.saveAssistantMessage(
                            result.data.conversationMessage
                        )

                        // 5. Возвращаем сообщение с правильным ID
                        emit(
                            NetworkResult.Success(
                                result.data.conversationMessage.copy(id = assistantId)
                            )
                        )
                    }
                    is NetworkResult.Error -> {
                        emit(NetworkResult.Error(result.error))
                    }
                    is NetworkResult.Loading -> {
                        emit(NetworkResult.Loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(
                NetworkResult.Error(
                    DomainError.DatabaseError(
                        operation = "sendMessage",
                        message = "Ошибка при отправке сообщения: ${e.message}",
                        exception = e
                    )
                )
            )
        }
    }
}