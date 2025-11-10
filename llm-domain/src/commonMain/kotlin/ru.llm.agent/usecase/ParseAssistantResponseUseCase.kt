package ru.llm.agent.usecase

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.llm.agent.model.AssistantJsonAnswer

/**
 * Use case для парсинга JSON ответа от ассистента.
 * Выносит бизнес-логику парсинга из repository layer.
 */
public class ParseAssistantResponseUseCase {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Парсит текстовый ответ от LLM в структурированный объект.
     * Автоматически очищает backticks и markdown форматирование.
     *
     * @param rawResponse Сырой текст ответа от LLM
     * @return Результат парсинга: Success с данными или Failure с ошибкой
     */
    public operator fun invoke(rawResponse: String): Result<AssistantJsonAnswer> {
        return try {
            // Очищаем backticks из начала и конца строки
            val cleanedResponse = rawResponse
                .replace(Regex("^`+json\\s*"), "")  // Удаляем ```json в начале
                .replace(Regex("^`+"), "")          // Удаляем ``` в начале
                .replace(Regex("`+$"), "")          // Удаляем ``` в конце
                .trim()

            val parsed = json.decodeFromString<AssistantJsonAnswer>(cleanedResponse)
            Result.success(parsed)
        } catch (e: SerializationException) {
            Result.failure(
                ParseException("Не удалось распарсить JSON ответ: ${e.message}", rawResponse, e)
            )
        } catch (e: IllegalArgumentException) {
            Result.failure(
                ParseException("Некорректный формат JSON: ${e.message}", rawResponse, e)
            )
        }
    }

    /**
     * Исключение, возникающее при ошибке парсинга ответа ассистента
     */
    public class ParseException(
        message: String,
        public val rawResponse: String,
        cause: Throwable? = null
    ) : Exception(message, cause)
}
