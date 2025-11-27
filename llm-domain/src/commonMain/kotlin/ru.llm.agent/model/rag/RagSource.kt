package ru.llm.agent.model.rag

/**
 * Источник RAG, использованный при генерации ответа.
 * Хранит информацию о документе из базы знаний, который был использован для ответа.
 */
public data class RagSource(
    /** ID источника в БД */
    public val id: Long = 0,
    /** ID сообщения ассистента, к которому относится источник */
    public val messageId: Long,
    /** Индекс источника в ответе (для ссылок [1], [2], [3]) */
    public val index: Int,
    /** Текст цитаты из документа */
    public val text: String,
    /** ID исходного документа (например, имя файла) */
    public val sourceId: String,
    /** Индекс чанка в исходном документе */
    public val chunkIndex: Int,
    /** Релевантность документа (0.0 - 1.0) */
    public val similarity: Double,
    /** Временная метка */
    public val timestamp: Long = System.currentTimeMillis()
)
