package ru.llm.agent.mcp.model

/**
 * Статус выполнения MCP инструмента
 */
data class McpToolExecutionStatus(
    /** Название инструмента */
    val toolName: String,
    /** Описание выполняемой операции */
    val description: String,
    /** Флаг выполнения (анимация индикатора) */
    val isExecuting: Boolean = true,
    /** Результат выполнения (если завершён) */
    val result: String? = null,
    /** Ошибка выполнения (если произошла) */
    val error: String? = null
)