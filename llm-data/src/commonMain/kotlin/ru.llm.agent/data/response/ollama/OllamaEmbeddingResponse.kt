package ru.llm.agent.data.response.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ от Ollama API с эмбеддингом
 * Ollama возвращает embeddings как массив массивов (для батч-запросов)
 */
@Serializable
public data class OllamaEmbeddingResponse(
    val model: String,
    @SerialName("embeddings")
    val embeddings: List<List<Double>>
) {
    /**
     * Получить первый эмбеддинг из ответа
     * (для single-input запросов)
     */
    public fun getEmbedding(): List<Double> = embeddings.firstOrNull() ?: emptyList()
}