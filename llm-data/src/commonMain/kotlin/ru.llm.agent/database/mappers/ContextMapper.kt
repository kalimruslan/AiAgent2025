package ru.llm.agent.database.mappers

import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.database.context.ContextEntity

/**
 * Mapper для преобразования между ContextEntity (БД) и Domain моделями контекста.
 * Следует принципу Single Responsibility и обеспечивает чёткое разделение слоёв.
 */
public object ContextMapper {
    /**
     * Data class для представления контекста в Domain слое
     */
    public data class ContextDomain(
        val conversationId: ConversationId,
        val temperature: Double,
        val systemPrompt: String,
        val maxTokens: TokenCount,
        val timestamp: Long,
        val llmProvider: String?
    )

    /**
     * Преобразовать ContextEntity в Domain модель
     */
    public fun toDomain(entity: ContextEntity): ContextDomain {
        return ContextDomain(
            conversationId = ConversationId(entity.conversationId),
            temperature = entity.temperature,
            systemPrompt = entity.systemprompt,
            maxTokens = TokenCount(entity.maxTokens),
            timestamp = entity.timestamp,
            llmProvider = entity.llmProvider
        )
    }

    /**
     * Преобразовать Domain модель в ContextEntity
     */
    public fun toEntity(domain: ContextDomain, id: Long = 0): ContextEntity {
        return ContextEntity(
            id = id,
            conversationId = domain.conversationId.value,
            temperature = domain.temperature,
            systemprompt = domain.systemPrompt,
            maxTokens = domain.maxTokens.value,
            timestamp = domain.timestamp,
            llmProvider = domain.llmProvider
        )
    }

    /**
     * Преобразовать список entities в список domain моделей
     */
    public fun toDomainList(entities: List<ContextEntity>): List<ContextDomain> {
        return entities.map { toDomain(it) }
    }
}
