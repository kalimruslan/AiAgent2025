package ru.llm.agent.service

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Сервис для отправки сообщений через различные LLM провайдеры.
 * Инкапсулирует логику взаимодействия с API разных провайдеров.
 */
public interface MessageSendingService {
    /**
     * Отправить сообщения через указанный провайдер
     *
     * @param conversationId ID диалога
     * @param messages Список сообщений для отправки (включая системный промпт и историю)
     * @param provider Провайдер LLM для отправки
     * @param temperature Температура для генерации (0.0 - 1.0), null = использовать значение по умолчанию
     * @param maxTokens Максимальное количество токенов в ответе, null = использовать значение по умолчанию
     * @return Flow с результатом отправки сообщения
     */
    public suspend fun sendMessages(
        conversationId: String,
        messages: List<ConversationMessage>,
        provider: LlmProvider,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Flow<NetworkResult<MessageSendingResult>>
}

/**
 * Результат отправки сообщения через LLM провайдер
 *
 * @param conversationMessage Сформированное сообщение для сохранения в БД
 * @param rawResponse Сырой ответ от API (для отладки и логирования)
 */
public data class MessageSendingResult(
    val conversationMessage: ConversationMessage,
    val rawResponse: String
)
