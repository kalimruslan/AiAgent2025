package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.database.mappers.ContextMapper
import ru.llm.agent.database.mappers.MessageMapper
import ru.llm.agent.repository.TokenManagementRepository
import ru.llm.agent.repository.TokenUsage

/**
 * Реализация TokenManagementRepository с использованием Room Database.
 * Вычисляет использование токенов на основе сообщений и настроек контекста.
 */
public class TokenManagementRepositoryImpl(
    private val database: MessageDatabase
) : TokenManagementRepository {

    private val messageReadDao = database.messageReadDao()
    private val contextReadDao = database.contextReadDao()

    private val messageWriteDao = database.messageWriteDao()

    override suspend fun getTokenUsage(conversationId: ConversationId): Flow<TokenUsage> {
        val messagesFlow = messageReadDao.getMessagesByConversation(conversationId.value)
            .map { entities -> MessageMapper.toDomainList(entities) }

        val contextFlow = contextReadDao.getContextByConversationIdFlow(conversationId.value)
            .map { entity -> entity?.let { ContextMapper.toDomain(it) } }

        return combine(messagesFlow, contextFlow) { messages, context ->
            // Подсчитываем общее количество токенов из всех сообщений
            val currentTokens = messages
                .mapNotNull { it.totalTokens }
                .fold(TokenCount(0)) { acc, tokens -> acc + tokens }

            // Получаем максимальное количество токенов из контекста
            val maxTokens = context?.maxTokens ?: TokenCount(4000) // Дефолтное значение

            // Вычисляем процент использования
            val usageRatio = if (maxTokens.value > 0) {
                currentTokens.value.toDouble() / maxTokens.value.toDouble()
            } else {
                0.0
            }

            TokenUsage(
                currentTokens = currentTokens,
                maxTokens = maxTokens,
                usageRatio = usageRatio,
                messageCount = messages.size
            )
        }
    }

    override suspend fun getTokenUsageSync(conversationId: ConversationId): TokenUsage {
        val entities = messageReadDao.getMessagesByConversationSync(conversationId.value)
        val messages = MessageMapper.toDomainList(entities)

        val currentTokens = messages
            .mapNotNull { it.totalTokens }
            .fold(TokenCount(0)) { acc, tokens -> acc + tokens }

        val contextEntity = contextReadDao.getContextByConversationId(conversationId.value)
        val maxTokens = contextEntity?.let { TokenCount(it.maxTokens) } ?: TokenCount(4000)

        val usageRatio = if (maxTokens.value > 0) {
            currentTokens.value.toDouble() / maxTokens.value.toDouble()
        } else {
            0.0
        }

        return TokenUsage(
            currentTokens = currentTokens,
            maxTokens = maxTokens,
            usageRatio = usageRatio,
            messageCount = messages.size
        )
    }

    override suspend fun countTokens(text: String, model: String): TokenCount {
        // Упрощённый подсчёт токенов: ~4 символа на токен
        // В реальности нужно использовать tokenizer для конкретной модели
        val estimatedTokens = text.length / 4
        return TokenCount(estimatedTokens.coerceAtLeast(1))
    }

    override suspend fun updateMessageTokens(
        conversationId: ConversationId,
        messageId: Long,
        inputTokens: TokenCount?,
        completionTokens: TokenCount?,
        totalTokens: TokenCount?
    ) {
        val message = messageReadDao.getMessageById(messageId) ?: return

        val updatedMessage = message.copy(
            inputTokens = inputTokens?.value,
            completionTokens = completionTokens?.value,
            totalTokens = totalTokens?.value
        )

        messageWriteDao.updateMessage(updatedMessage)
    }

    override suspend fun needsSummarization(
        conversationId: ConversationId,
        threshold: Double
    ): Boolean {
        val usage = getTokenUsageSync(conversationId)
        return usage.usageRatio >= threshold
    }
}