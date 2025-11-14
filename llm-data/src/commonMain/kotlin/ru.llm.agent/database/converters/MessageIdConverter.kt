package ru.llm.agent.database.converters

import androidx.room.TypeConverter
import ru.llm.agent.core.utils.model.MessageId

/**
 * Room TypeConverter для MessageId value class.
 * Позволяет использовать MessageId в Entity классах.
 */
public class MessageIdConverter {
    /**
     * Преобразует MessageId в Long для сохранения в БД
     */
    @TypeConverter
    public fun fromMessageId(messageId: MessageId?): Long? {
        return messageId?.value
    }

    /**
     * Преобразует Long из БД в MessageId
     */
    @TypeConverter
    public fun toMessageId(value: Long?): MessageId? {
        return value?.let {
            if (it > 0) MessageId(it) else null
        }
    }
}