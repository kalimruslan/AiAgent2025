package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.llm.agent.repository.ConversationRepository

/**
 * Модель использования токенов
 */
public data class TokenUsage(
    /** Использованные токены (сумма totalTokens из всех сообщений) */
    val usedTokens: Int,
    /** Максимально допустимое количество токенов из контекста */
    val maxTokens: Int
)

/**
 * Use case для получения информации об использовании токенов в диалоге
 */
public class GetTokenUsageUseCase(
    private val conversationRepository: ConversationRepository
) {
    /**
     * Получить использование токенов для указанного диалога
     *
     * @param conversationId ID диалога
     * @return Flow с информацией об использовании токенов
     */
    public suspend operator fun invoke(conversationId: String): Flow<TokenUsage> {
        // Комбинируем сообщения и контекст
        return conversationRepository.getMessages(conversationId).combine(
            conversationRepository.getContext(conversationId)
        ) { messages, context ->
            // Суммируем totalTokens из всех сообщений
            val usedTokens = messages.sumOf { message ->
                message.totalTokens ?: 0
            }

            // Берем maxTokens из контекста, если нет - используем значение по умолчанию
            val maxTokens = context?.maxTokens ?: 8000

            TokenUsage(
                usedTokens = usedTokens,
                maxTokens = maxTokens
            )
        }
    }
}