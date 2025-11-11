package ru.llm.agent.core.utils

/**
 * Кроссплатформенный интерфейс логгера для соблюдения Clean Architecture.
 * Позволяет логировать без platform-specific зависимостей в domain layer.
 */
public interface Logger {
    /**
     * Логирование информационного сообщения
     */
    public fun info(message: String)

    /**
     * Логирование предупреждения
     */
    public fun warning(message: String)

    /**
     * Логирование ошибки
     */
    public fun error(message: String, throwable: Throwable? = null)

    /**
     * Логирование отладочного сообщения
     */
    public fun debug(message: String)
}

/**
 * Фабрика для создания platform-specific экземпляров логгера
 */
public expect fun createLogger(tag: String): Logger

/**
 * Extension для упрощения использования warning как warn
 */
public fun Logger.warn(message: String) {
    warning(message)
}
