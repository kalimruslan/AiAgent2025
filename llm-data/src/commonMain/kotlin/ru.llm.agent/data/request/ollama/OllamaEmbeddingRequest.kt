package ru.llm.agent.data.request.ollama

import kotlinx.serialization.Serializable

/**
 * Запрос для получения эмбеддинга от Ollama API
 */
@Serializable
public data class OllamaEmbeddingRequest(
    val model: String,
    val input: String
)