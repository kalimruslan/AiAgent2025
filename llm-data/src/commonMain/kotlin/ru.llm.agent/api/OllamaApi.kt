package ru.llm.agent.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.llm.agent.data.request.ollama.OllamaEmbeddingRequest
import ru.llm.agent.data.response.ollama.OllamaEmbeddingResponse

/**
 * API клиент для работы с Ollama
 * Ollama по умолчанию работает на http://localhost:11434
 */
public class OllamaApi(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:11434"
) {
    /**
     * Получить эмбеддинг для текста
     * @param text текст для эмбеддинга
     * @param model модель эмбеддинга (по умолчанию nomic-embed-text)
     * @return вектор эмбеддинга
     */
    public suspend fun getEmbedding(
        text: String,
        model: String = "nomic-embed-text"
    ): OllamaEmbeddingResponse {
        return client.post("$baseUrl/api/embed") {
            contentType(ContentType.Application.Json)
            setBody(
                OllamaEmbeddingRequest(
                    model = model,
                    input = text
                )
            )
        }.body()
    }
}