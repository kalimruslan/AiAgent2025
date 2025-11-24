package ru.llm.agent.model.rag

/**
 * Модель документа для RAG системы (domain layer)
 */
public data class RagDocument(
    val text: String,
    val similarity: Double,
    val metadata: Map<String, String>
)