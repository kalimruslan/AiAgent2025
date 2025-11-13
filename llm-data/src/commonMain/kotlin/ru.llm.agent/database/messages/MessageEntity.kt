package ru.llm.agent.database.messages

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
public data class MessageEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val role: String, // "system", "user", "assistant"
    val text: String,
    val timestamp: Long,
    val originalResponse: String? = null,
    val model: String,
    /** Количество входных токенов (из запроса) */
    val inputTokens: Int? = null,
    /** Количество токенов в ответе (completion tokens) */
    val completionTokens: Int? = null,
    /** Общее количество токенов */
    val totalTokens: Int? = null,
    /** Время ответа LLM в миллисекундах */
    val responseTimeMs: Long? = null,
    /** Флаг суммаризированного сообщения (история диалога была сжата в это сообщение) */
    val isSummarized: Boolean = false
)