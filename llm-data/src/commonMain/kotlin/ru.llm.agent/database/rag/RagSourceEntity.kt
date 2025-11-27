package ru.llm.agent.database.rag

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения источников RAG, использованных при генерации ответа.
 * Связывает ответ ассистента с документами из базы знаний.
 */
@Entity(tableName = "rag_sources")
public data class RagSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID сообщения ассистента, к которому относится источник */
    val messageId: Long,

    /** ID диалога (для группировки и удаления) */
    val conversationId: String,

    /** Индекс источника в ответе (1, 2, 3...) для ссылок [1], [2], [3] */
    val index: Int,

    /** Текст цитаты из документа */
    val text: String,

    /** ID исходного документа (например, имя файла) */
    val sourceId: String,

    /** Индекс чанка в исходном документе */
    val chunkIndex: Int,

    /** Релевантность документа (0.0 - 1.0) */
    val similarity: Double,

    /** Временная метка создания */
    val timestamp: Long = System.currentTimeMillis()
)
