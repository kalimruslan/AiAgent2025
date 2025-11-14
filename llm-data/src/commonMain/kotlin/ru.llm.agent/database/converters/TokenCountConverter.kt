package ru.llm.agent.database.converters

import androidx.room.TypeConverter
import ru.llm.agent.core.utils.model.TokenCount

/**
 * Room TypeConverter для TokenCount value class.
 * Позволяет использовать TokenCount в Entity классах.
 */
public class TokenCountConverter {
    /**
     * Преобразует TokenCount в Int для сохранения в БД
     */
    @TypeConverter
    public fun fromTokenCount(tokenCount: TokenCount?): Int? {
        return tokenCount?.value
    }

    /**
     * Преобразует Int из БД в TokenCount
     */
    @TypeConverter
    public fun toTokenCount(value: Int?): TokenCount? {
        return value?.let {
            if (it >= 0) TokenCount(it) else null
        }
    }
}