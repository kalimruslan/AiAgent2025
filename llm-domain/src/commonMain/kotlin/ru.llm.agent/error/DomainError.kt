package ru.llm.agent.error

/**
 * Базовый sealed класс для всех доменных ошибок.
 * Обеспечивает типизированную обработку ошибок во всех слоях приложения.
 *
 * Каждая ошибка имеет уникальный код для категоризации и отслеживания.
 */
public sealed class DomainError {
    /**
     * Уникальный код ошибки для категоризации и отслеживания
     */
    public abstract val errorCode: String

    /**
     * Временная метка возникновения ошибки (для отладки и аналитики)
     */
    public open val timestamp: Long = System.currentTimeMillis()
    /**
     * Ошибка сети или HTTP запроса
     * @param code HTTP код ошибки (например, 404, 500)
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение, если есть
     * @param endpoint URL или эндпоинт, на котором произошла ошибка (для контекста)
     */
    public data class NetworkError(
        val code: Int?,
        val message: String,
        val exception: Throwable? = null,
        val endpoint: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "NET_${code ?: "UNKNOWN"}"
    }

    /**
     * Ошибка парсинга данных (JSON, XML и т.д.)
     * @param rawData Сырые данные, которые не удалось распарсить
     * @param message Описание ошибки парсинга
     * @param exception Исходное исключение парсинга
     * @param dataType Тип данных, который пытались распарсить (для контекста)
     */
    public data class ParseError(
        val rawData: String,
        val message: String,
        val exception: Throwable? = null,
        val dataType: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "PARSE_ERROR"
    }

    /**
     * Ошибка работы с базой данных
     * @param operation Операция, которая провалилась (например, "insert", "query", "delete")
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение
     * @param table Имя таблицы (для контекста)
     */
    public data class DatabaseError(
        val operation: String,
        val message: String,
        val exception: Throwable,
        val table: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "DB_${operation.uppercase()}"
    }

    /**
     * Ошибка валидации данных
     * @param field Поле, которое не прошло валидацию
     * @param message Сообщение об ошибке валидации
     * @param value Значение, которое не прошло валидацию (опционально)
     * @param constraint Правило валидации, которое было нарушено
     */
    public data class ValidationError(
        val field: String,
        val message: String,
        val value: Any? = null,
        val constraint: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "VALIDATION_${field.uppercase()}"
    }

    /**
     * Ошибка бизнес-логики
     * @param reason Причина ошибки (код причины для категоризации)
     * @param message Сообщение об ошибке
     * @param context Дополнительный контекст ошибки
     */
    public data class BusinessLogicError(
        val reason: String,
        val message: String,
        val context: Map<String, Any?>? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "BIZ_${reason.uppercase()}"
    }

    /**
     * Неизвестная или непредвиденная ошибка
     * @param message Сообщение об ошибке
     * @param exception Исходное исключение
     * @param source Источник ошибки (для контекста)
     */
    public data class UnknownError(
        val message: String,
        val exception: Throwable? = null,
        val source: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "UNKNOWN_ERROR"
    }

    /**
     * Ошибка конфигурации (например, отсутствие API ключа)
     * @param parameter Параметр конфигурации, который отсутствует или неверен
     * @param message Сообщение об ошибке
     * @param expectedValue Ожидаемое значение или формат (для контекста)
     */
    public data class ConfigurationError(
        val parameter: String,
        val message: String,
        val expectedValue: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DomainError() {
        override val errorCode: String = "CONFIG_${parameter.uppercase()}"
    }

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
     * с полным контекстом и stacktrace
     */
    public fun toLogMessage(): String = when (this) {
        is NetworkError -> buildString {
            append("[$errorCode] NetworkError(")
            append("code=$code, ")
            append("message=$message, ")
            endpoint?.let { append("endpoint=$it, ") }
            append("timestamp=$timestamp")
            exception?.let { append(", exception=${it.stackTraceToString()}") }
            append(")")
        }
        is ParseError -> buildString {
            append("[$errorCode] ParseError(")
            append("message=$message, ")
            dataType?.let { append("dataType=$it, ") }
            append("rawData=${rawData.take(200).replace("\n", "\\n")}..., ")
            append("timestamp=$timestamp")
            exception?.let { append(", exception=${it.stackTraceToString()}") }
            append(")")
        }
        is DatabaseError -> buildString {
            append("[$errorCode] DatabaseError(")
            append("operation=$operation, ")
            table?.let { append("table=$it, ") }
            append("message=$message, ")
            append("timestamp=$timestamp, ")
            append("exception=${exception.stackTraceToString()}")
            append(")")
        }
        is ValidationError -> buildString {
            append("[$errorCode] ValidationError(")
            append("field=$field, ")
            append("message=$message, ")
            append("value=$value, ")
            constraint?.let { append("constraint=$it, ") }
            append("timestamp=$timestamp")
            append(")")
        }
        is BusinessLogicError -> buildString {
            append("[$errorCode] BusinessLogicError(")
            append("reason=$reason, ")
            append("message=$message, ")
            context?.let { append("context=$it, ") }
            append("timestamp=$timestamp")
            append(")")
        }
        is UnknownError -> buildString {
            append("[$errorCode] UnknownError(")
            append("message=$message, ")
            source?.let { append("source=$it, ") }
            append("timestamp=$timestamp")
            exception?.let { append(", exception=${it.stackTraceToString()}") }
            append(")")
        }
        is ConfigurationError -> buildString {
            append("[$errorCode] ConfigurationError(")
            append("parameter=$parameter, ")
            append("message=$message, ")
            expectedValue?.let { append("expectedValue=$it, ") }
            append("timestamp=$timestamp")
            append(")")
        }
    }

    /**
     * Получить stacktrace из exception, если он есть
     */
    public fun getStackTrace(): String? = when (this) {
        is NetworkError -> exception?.stackTraceToString()
        is ParseError -> exception?.stackTraceToString()
        is DatabaseError -> exception.stackTraceToString()
        is UnknownError -> exception?.stackTraceToString()
        else -> null
    }
}

/**
 * Extension функция для преобразования Throwable в DomainError
 */
public fun Throwable.toDomainError(): DomainError = DomainError.UnknownError(
    message = this.message ?: "Неизвестная ошибка",
    exception = this
)