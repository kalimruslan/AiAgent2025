package ru.llm.agent.data.response.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ от Ollama API с эмбеддингом
 * Ollama возвращает embeddings как массив массивов (для батч-запросов)
 */
@Serializable
public data class OllamaEmbeddingResponse(
    val model: String? = null,
    @SerialName("embeddings")
    val embeddings: List<List<Double>>? = null,
    // Поля для ошибок
    val error: String? = null
) {
    /**
     * Получить первый эмбеддинг из ответа
     * (для single-input запросов)
     */
    public fun getEmbedding(): List<Double> {
        if (error != null) {
            throw IllegalStateException("Ollama API error: $error")
        }
        return embeddings?.firstOrNull() ?: emptyList()
    }

    /**
     * Проверить, есть ли ошибка в ответе
     */
    public fun hasError(): Boolean = error != null

    /**
     * Получить модель (или дефолтное значение при ошибке)
     */
    public fun getModelName(): String = model ?: "unknown"
}