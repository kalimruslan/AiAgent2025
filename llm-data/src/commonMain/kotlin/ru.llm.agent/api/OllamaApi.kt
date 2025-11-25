package ru.llm.agent.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import ru.llm.agent.data.request.ollama.OllamaEmbeddingRequest
import ru.llm.agent.data.response.ollama.OllamaEmbeddingResponse
import java.util.logging.Logger

/**
 * API клиент для работы с Ollama
 * Ollama по умолчанию работает на http://localhost:11434
 */
public class OllamaApi(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:11434"
) {
    private val logger = Logger.getLogger("OllamaApi")

    /**
     * Получить эмбеддинг для текста с автоматическими повторными попытками
     * @param text текст для эмбеддинга
     * @param model модель эмбеддинга (по умолчанию mxbai-embed-large - поддерживает до 512 токенов)
     * @param maxRetries максимальное количество попыток
     * @return вектор эмбеддинга
     */
    public suspend fun getEmbedding(
        text: String,
        model: String = "mxbai-embed-large",
        maxRetries: Int = 3
    ): OllamaEmbeddingResponse {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val textLength = text.length
                val textPreview = if (text.length > 100) text.take(100) + "..." else text
                logger.info("Запрос эмбеддинга для текста длиной $textLength символов (модель: $model)${if (attempt > 0) ", попытка ${attempt + 1}/$maxRetries" else ""}")
                logger.fine("Превью текста: $textPreview")

                val response = client.post("$baseUrl/api/embed") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        OllamaEmbeddingRequest(
                            model = model,
                            input = text
                        )
                    )
                }

                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText()

                    // Проверяем, является ли ошибка EOF - это временная ошибка, можно повторить
                    if (errorBody.contains("EOF") && attempt < maxRetries - 1) {
                        logger.warning("Ollama EOF ошибка (попытка ${attempt + 1}/$maxRetries), повторяем через 1 секунду...")
                        delay(1000L * (attempt + 1)) // Экспоненциальная задержка
                        lastException = IllegalStateException("Ollama API EOF error")
                        return@repeat // Продолжаем цикл
                    }

                    logger.warning("Ollama API вернул ошибку: ${response.status}, тело: $errorBody")
                    throw IllegalStateException("Ollama API error: ${response.status}, body: $errorBody")
                }

                val embeddingResponse: OllamaEmbeddingResponse = response.body()
                logger.info("Получен эмбеддинг размерности ${embeddingResponse.getEmbedding().size}")

                return embeddingResponse
            } catch (e: Exception) {
                lastException = e

                // Если это EOF или network ошибка и есть еще попытки - повторяем
                if ((e.message?.contains("EOF") == true || e.message?.contains("Connection") == true)
                    && attempt < maxRetries - 1) {
                    logger.warning("Ошибка сети (попытка ${attempt + 1}/$maxRetries): ${e.message}, повторяем...")
                    delay(1000L * (attempt + 1))
                    return@repeat
                }

                logger.severe("Ошибка получения эмбеддинга: ${e.message}")
                logger.severe("Тип ошибки: ${e::class.simpleName}")
                logger.severe("Длина текста: ${text.length}, превью: ${text.take(100)}...")

                // Если это последняя попытка - выбрасываем исключение
                if (attempt == maxRetries - 1) {
                    throw e
                }
            }
        }

        // Если дошли сюда - все попытки исчерпаны
        throw lastException ?: IllegalStateException("Не удалось получить эмбеддинг после $maxRetries попыток")
    }
}