package ru.llm.agent.database.mappers

import ru.llm.agent.database.expert.ExpertOpinionEntity
import ru.llm.agent.model.ExpertOpinion

/**
 * Mapper для преобразования между ExpertOpinionEntity (БД) и ExpertOpinion (Domain).
 * Следует принципу Single Responsibility и обеспечивает чёткое разделение слоёв.
 */
public object ExpertOpinionMapper {
    /**
     * Преобразовать ExpertOpinionEntity в Domain модель
     */
    public fun toDomain(entity: ExpertOpinionEntity): ExpertOpinion {
        return ExpertOpinion(
            id = entity.id,
            expertId = entity.expertId,
            expertName = entity.expertName,
            expertIcon = entity.expertIcon,
            messageId = entity.messageId,
            opinion = entity.opinion,
            timestamp = entity.timestamp,
            originalResponse = entity.originalResponse
        )
    }

    /**
     * Преобразовать Domain модель в ExpertOpinionEntity
     */
    public fun toEntity(opinion: ExpertOpinion, conversationId: String): ExpertOpinionEntity {
        return ExpertOpinionEntity(
            id = opinion.id,
            expertId = opinion.expertId,
            expertName = opinion.expertName,
            expertIcon = opinion.expertIcon,
            messageId = opinion.messageId,
            conversationId = conversationId,
            opinion = opinion.opinion,
            timestamp = opinion.timestamp,
            originalResponse = opinion.originalResponse
        )
    }

    /**
     * Преобразовать список entities в список domain моделей
     */
    public fun toDomainList(entities: List<ExpertOpinionEntity>): List<ExpertOpinion> {
        return entities.map { toDomain(it) }
    }

    /**
     * Преобразовать список domain моделей в список entities
     */
    public fun toEntityList(opinions: List<ExpertOpinion>, conversationId: String): List<ExpertOpinionEntity> {
        return opinions.map { toEntity(it, conversationId) }
    }
}