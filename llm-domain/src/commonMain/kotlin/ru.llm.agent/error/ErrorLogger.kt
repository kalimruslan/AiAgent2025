package ru.llm.agent.error

import ru.llm.agent.core.utils.Logger
import ru.llm.agent.core.utils.warn

/**
 * Централизованный логгер для обработки и логирования доменных ошибок.
 *
 * Предоставляет единую точку для логирования всех ошибок в приложении
 * с автоматическим форматированием, включением stacktrace и контекста.
 */
public class ErrorLogger(
    private val logger: Logger
) {
    /**
     * Логировать ошибку с полным контекстом
     *
     * @param error Доменная ошибка для логирования
     * @param context Дополнительный контекст (например, имя use case, параметры вызова)
     * @param severity Уровень серьезности (по умолчанию ERROR)
     */
    public fun logError(
        error: DomainError,
        context: String? = null,
        severity: ErrorSeverity = ErrorSeverity.ERROR
    ) {
        val logMessage = buildString {
            // Заголовок с severity и errorCode
            append("[${severity.name}] ")
            context?.let { append("[$it] ") }

            // Основное сообщение ошибки с полным контекстом
            appendLine(error.toLogMessage())

            // Stacktrace если есть
            error.getStackTrace()?.let {
                appendLine("Stack trace:")
                appendLine(it)
            }
        }

        // Логируем в зависимости от severity
        when (severity) {
            ErrorSeverity.WARNING -> logger.warn(logMessage)
            ErrorSeverity.ERROR -> logger.error(logMessage)
            ErrorSeverity.CRITICAL -> logger.error("⚠️ CRITICAL: $logMessage")
        }
    }

    /**
     * Логировать сетевую ошибку с дополнительным контекстом
     */
    public fun logNetworkError(
        error: DomainError.NetworkError,
        context: String? = null
    ) {
        val severity = when (error.code) {
            in 400..499 -> ErrorSeverity.WARNING // Клиентские ошибки
            in 500..599 -> ErrorSeverity.ERROR   // Серверные ошибки
            else -> ErrorSeverity.ERROR
        }
        logError(error, context, severity)
    }

    /**
     * Логировать ошибку парсинга с сохранением сырых данных
     */
    public fun logParseError(
        error: DomainError.ParseError,
        context: String? = null
    ) {
        // Парсинг критичен - всегда ERROR
        logError(error, context, ErrorSeverity.ERROR)
    }

    /**
     * Логировать ошибку БД как критическую
     */
    public fun logDatabaseError(
        error: DomainError.DatabaseError,
        context: String? = null
    ) {
        // БД ошибки всегда критичны
        logError(error, context, ErrorSeverity.CRITICAL)
    }

    /**
     * Логировать ошибку валидации как предупреждение
     */
    public fun logValidationError(
        error: DomainError.ValidationError,
        context: String? = null
    ) {
        // Валидация - это обычно предупреждение
        logError(error, context, ErrorSeverity.WARNING)
    }

    /**
     * Логировать неизвестную ошибку как критическую
     */
    public fun logUnknownError(
        error: DomainError.UnknownError,
        context: String? = null
    ) {
        // Неизвестные ошибки требуют внимания
        logError(error, context, ErrorSeverity.CRITICAL)
    }

    /**
     * Краткое логирование для некритичных ошибок
     * (без stacktrace, только основная информация)
     */
    public fun logBrief(error: DomainError, context: String? = null) {
        val message = buildString {
            append("[${error.errorCode}] ")
            context?.let { append("[$it] ") }
            append(error.toUserMessage())
        }
        logger.info(message)
    }
}

/**
 * Уровни серьезности ошибок
 */
public enum class ErrorSeverity {
    /** Предупреждение - некритичная ошибка, приложение может продолжить работу */
    WARNING,

    /** Ошибка - серьезная проблема, но приложение может восстановиться */
    ERROR,

    /** Критическая ошибка - требует немедленного внимания */
    CRITICAL
}