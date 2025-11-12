package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.llm.agent.NetworkResult
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository

/**
 * Use case для подсчёта токенов в сообщении перед отправкой
 *
 * Подсчитывает токены с учётом:
 * - Истории сообщений в диалоге
 * - System prompt
 * - Нового сообщения пользователя
 */
public class GetMessageTokenCountUseCase(
    private val llmRepository: LlmRepository,
    private val conversationRepository: ConversationRepository
) {
    /**
     * Подсчитать токены для нового сообщения с учётом контекста диалога
     *
     * @param conversationId ID диалога
     * @param newMessage Новое сообщение пользователя
     * @param modelUri URI модели YandexGPT (опционально)
     * @return Flow с результатом подсчёта токенов
     */
    public suspend operator fun invoke(
        conversationId: String,
        newMessage: String,
        modelUri: String? = null
    ): Flow<NetworkResult<Int>> {
        // Получаем текущие сообщения
        val messages = conversationRepository.getMessages(conversationId).first()

        // Получаем контекст (для system prompt)
        val context = conversationRepository.getContext(conversationId).first()

        // Формируем полный текст для подсчёта токенов:
        // System prompt + История сообщений + Новое сообщение
        val fullText = buildString {
            // Добавляем system prompt, если есть
            context?.systemPrompt?.let { systemPrompt ->
                if (systemPrompt.isNotBlank()) {
                    appendLine("System: $systemPrompt")
                    appendLine()
                }
            }

            // Добавляем историю сообщений
            messages.forEach { message ->
                appendLine("${message.role.title}: ${message.text}")
            }

            // Добавляем новое сообщение
            appendLine("user: $newMessage")
        }

        // Вызываем tokenizer API
        return llmRepository.countYandexGPTTokens(
            text = fullText,
            modelUri = modelUri
        )
    }
}