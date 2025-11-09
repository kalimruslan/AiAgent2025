package ru.llm.agent.core.utils

/**
 * Cross-platform logger interface for Clean Architecture compliance.
 * Allows logging without platform-specific dependencies in domain layer.
 */
public interface Logger {
    /**
     * Log informational message
     */
    public fun info(message: String)

    /**
     * Log warning message
     */
    public fun warning(message: String)

    /**
     * Log error message
     */
    public fun error(message: String, throwable: Throwable? = null)

    /**
     * Log debug message
     */
    public fun debug(message: String)
}

/**
 * Factory to create platform-specific logger instances
 */
public expect fun createLogger(tag: String): Logger
