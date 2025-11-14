package ru.llm.agent.database.converters

import androidx.room.TypeConverter
import ru.llm.agent.core.utils.model.ConversationId

/**
 * Room TypeConverter для ConversationId value class.
 * Позволяет использовать ConversationId в Entity классах.
 */
public class ConversationIdConverter {
    /**
     * Преобразует ConversationId в String для сохранения в БД
     */
    @TypeConverter
    public fun fromConversationId(conversationId: ConversationId?): String? {
        return conversationId?.value
    }

    /**
     * Преобразует String из БД в ConversationId
     */
    @TypeConverter
    public fun toConversationId(value: String?): ConversationId? {
        return value?.let { ConversationId(it) }
    }
}