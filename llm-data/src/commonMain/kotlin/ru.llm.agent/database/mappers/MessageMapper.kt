package ru.llm.agent.database.mappers

import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.model.conversation.Message

/**
 * Mapper для преобразования между MessageEntity (БД) и Message (Domain).
 * Следует принципу Single Responsibility и обеспечивает чёткое разделение слоёв.
 */
public object MessageMapper {
    /**
     * Преобразовать MessageEntity в Domain модель Message
     */
    public fun toDomain(entity: MessageEntity): Message {
        val conversationId = ConversationId(entity.conversationId)
        val messageId = MessageId(entity.id)

        return when (entity.role) {
            "user" -> Message.User(
                id = messageId,
                conversationId = conversationId,
                text = entity.text,
                timestamp = entity.timestamp,
                inputTokens = entity.inputTokens?.let { TokenCount(it) },
                totalTokens = entity.totalTokens?.let { TokenCount(it) },
                isSummarized = entity.isSummarized
            )

            "assistant" -> Message.Assistant(
                id = messageId,
                conversationId = conversationId,
                text = entity.text,
                model = entity.model,
                timestamp = entity.timestamp,
                originalResponse = entity.originalResponse,
                expertOpinions = emptyList(), // Будут загружены отдельно через ExpertOpinionDao
                inputTokens = entity.inputTokens?.let { TokenCount(it) },
                completionTokens = entity.completionTokens?.let { TokenCount(it) },
                totalTokens = entity.totalTokens?.let { TokenCount(it) },
                responseTimeMs = entity.responseTimeMs,
                isSummarized = entity.isSummarized
            )

            "system" -> Message.System(
                id = messageId,
                conversationId = conversationId,
                text = entity.text,
                timestamp = entity.timestamp,
                isSummarized = entity.isSummarized
            )

            else -> throw IllegalArgumentException("Неизвестная роль сообщения: ${entity.role}")
        }
    }

    /**
     * Преобразовать Domain модель Message в MessageEntity
     */
    public fun toEntity(message: Message): MessageEntity {
        val role = when (message) {
            is Message.User -> "user"
            is Message.Assistant -> "assistant"
            is Message.System -> "system"
        }

        return MessageEntity(
            id = message.id.value,
            conversationId = message.conversationId.value,
            role = role,
            text = message.text,
            timestamp = message.timestamp,
            model = message.model ?: "",
            originalResponse = when (message) {
                is Message.Assistant -> message.originalResponse
                else -> null
            },
            inputTokens = message.inputTokens?.value,
            completionTokens = message.completionTokens?.value,
            totalTokens = message.totalTokens?.value,
            responseTimeMs = message.responseTimeMs,
            isSummarized = message.isSummarized
        )
    }

    /**
     * Преобразовать список entities в список domain моделей
     */
    public fun toDomainList(entities: List<MessageEntity>): List<Message> {
        return entities.map { toDomain(it) }
    }

    /**
     * Преобразовать список domain моделей в список entities
     */
    public fun toEntityList(messages: List<Message>): List<MessageEntity> {
        return messages.map { toEntity(it) }
    }
}
