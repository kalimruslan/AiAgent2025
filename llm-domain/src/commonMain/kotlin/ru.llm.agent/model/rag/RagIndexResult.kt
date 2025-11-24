package ru.llm.agent.model.rag

/**
 * Результат индексации документа
 */
public data class RagIndexResult(
    val chunksIndexed: Int,
    val sourceId: String
)