package ru.llm.agent.database.expert

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица для хранения мнений экспертов
 */
@Entity(tableName = "expert_opinions")
public data class ExpertOpinionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID эксперта */
    val expertId: String,

    /** Название эксперта */
    val expertName: String,

    /** Иконка эксперта */
    val expertIcon: String,

    /** ID сообщения пользователя, к которому относится мнение */
    val messageId: Long,

    /** ID диалога */
    val conversationId: String,

    /** Текст мнения эксперта */
    val opinion: String,

    /** Временная метка */
    val timestamp: Long,

    /** Оригинальный JSON ответ от LLM */
    val originalResponse: String? = null
)
