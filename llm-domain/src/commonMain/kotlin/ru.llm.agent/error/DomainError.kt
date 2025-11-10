package ru.llm.agent.error

/**
 * Базовый sealed класс для всех доменных ошибок.
 * Обеспечивает типизированную обработку ошибок во всех слоях приложения.
 */
public sealed class DomainError {
    /**
     * Ошибка сети или HTTP запроса
     * @param code HTTP код ошибки (например, 404, 500)
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение, если есть
     */
    public data class NetworkError(
        val code: Int?,
        val message: String,
        val exception: Throwable? = null
    ) : DomainError()

    /**
     * Ошибка парсинга данных (JSON, XML и т.д.)
     * @param rawData Сырые данные, которые не удалось распарсить
     * @param message Описание ошибки парсинга
     * @param exception Исходное исключение парсинга
     */
    public data class ParseError(
        val rawData: String,
        val message: String,
        val exception: Throwable? = null
    ) : DomainError()

    /**
     * Ошибка работы с базой данных
     * @param operation Операция, которая провалилась (например, "insert", "query", "delete")
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение
     */
    public data class DatabaseError(
        val operation: String,
        val message: String,
        val exception: Throwable
    ) : DomainError()

    /**
     * Ошибка валидации данных
     * @param field Поле, которое не прошло валидацию
     * @param message Сообщение об ошибке валидации
     * @param value Значение, которое не прошло валидацию (опционально)
     */
    public data class ValidationError(
        val field: String,
        val message: String,
        val value: Any? = null
    ) : DomainError()

    /**
     * Ошибка бизнес-логики
     * @param reason Причина ошибки
     * @param message Сообщение об ошибке
     */
    public data class BusinessLogicError(
        val reason: String,
        val message: String
    ) : DomainError()

    /**
     * Неизвестная или непредвиденная ошибка
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение
     */
    public data class UnknownError(
        val message: String,
        val exception: Throwable? = null
    ) : DomainError()

    /**
     * Ошибка конфигурации (например, отсутствие API ключа)
     * @param parameter Параметр конфигурации, который отсутствует или неверен
     * @param message Сообщение об ошибке
     */
    public data class ConfigurationError(
        val parameter: String,
        val message: String
    ) : DomainError()

    /**
     * Преобразует DomainError в читаемое сообщение для пользователя
     */
    public fun toUserMessage(): String = when (this) {
        is NetworkError -> "Ошибка сети: $message${code?.let { " (код: $it)" } ?: ""}"
        is ParseError -> "Ошибка обработки данных: $message"
        is DatabaseError -> "Ошибка базы данных: $message"
        is ValidationError -> "Ошибка валидации поля '$field': $message"
        is BusinessLogicError -> "Ошибка: $message"
        is UnknownError -> "Неизвестная ошибка: $message"
        is ConfigurationError -> "Ошибка конфигурации '$parameter': $message"
    }

    /**
     * Преобразует DomainError в техническое сообщение для логирования
     */
    public fun toLogMessage(): String = when (this) {
        is NetworkError -> "NetworkError(code=$code, message=$message, exception=${exception?.message})"
        is ParseError -> "ParseError(message=$message, rawData=${rawData.take(100)}..., exception=${exception?.message})"
        is DatabaseError -> "DatabaseError(operation=$operation, message=$message, exception=${exception.message})"
        is ValidationError -> "ValidationError(field=$field, message=$message, value=$value)"
        is BusinessLogicError -> "BusinessLogicError(reason=$reason, message=$message)"
        is UnknownError -> "UnknownError(message=$message, exception=${exception?.message})"
        is ConfigurationError -> "ConfigurationError(parameter=$parameter, message=$message)"
    }
}

/**
 * Extension функция для преобразования Throwable в DomainError
 */
public fun Throwable.toDomainError(): DomainError = DomainError.UnknownError(
    message = this.message ?: "Неизвестная ошибка",
    exception = this
)