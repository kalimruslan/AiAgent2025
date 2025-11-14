package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.TokenCount

/**
 * Репозиторий для управления токенами диалога.
 * Отвечает за подсчёт, отслеживание и управление использованием токенов.
 */
public interface TokenManagementRepository {
    /**
     * Получить текущее использование токенов для диалога
     */
    public suspend fun getTokenUsage(conversationId: ConversationId): Flow<TokenUsage>

    /**
     * Получить текущее использование токенов синхронно
     */
    public suspend fun getTokenUsageSync(conversationId: ConversationId): TokenUsage

    /**
     * Подсчитать токены в тексте
     */
    public suspend fun countTokens(text: String, model: String): TokenCount

    /**
     * Обновить количество токенов для сообщения
     */
    public suspend fun updateMessageTokens(
        conversationId: ConversationId,
        messageId: Long,
        inputTokens: TokenCount?,
        completionTokens: TokenCount?,
        totalTokens: TokenCount?
    )

    /**
     * Проверить, нужна ли суммаризация на основе использования токенов
     */
    public suspend fun needsSummarization(
        conversationId: ConversationId,
        threshold: Double = 0.75
    ): Boolean
}

/**
 * Данные об использовании токенов в диалоге
 */
public data class TokenUsage(
    /** Текущее количество использованных токенов */
    val currentTokens: TokenCount,
    /** Максимальное количество токенов для контекста */
    val maxTokens: TokenCount,
    /** Процент использования контекста (0.0 - 1.0) */
    val usageRatio: Double,
    /** Количество сообщений в диалоге */
    val messageCount: Int
) {
    /** Процент использования в удобном формате (0-100) */
    val usagePercent: Float
        get() = (usageRatio * 100).toFloat()

    /** Достигнут ли лимит токенов */
    val isAtLimit: Boolean
        get() = currentTokens >= maxTokens

    /** Близко ли к лимиту (больше 75%) */
    val isNearLimit: Boolean
        get() = usageRatio >= 0.75
}