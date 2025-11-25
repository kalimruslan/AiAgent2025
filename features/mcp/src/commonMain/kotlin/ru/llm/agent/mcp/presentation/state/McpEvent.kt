package ru.llm.agent.mcp.presentation.state

/**
 * События для управления MCP функциональностью
 */
sealed interface McpEvent {
    /** Загрузить список доступных инструментов */
    data object LoadTools : McpEvent

    /** Переключить использование MCP инструментов */
    data class ToggleEnabled(val enabled: Boolean) : McpEvent

    /** Выполнить инструмент */
    data class ExecuteTool(
        val toolName: String,
        val arguments: Map<String, Any>
    ) : McpEvent

    /** Отменить выполнение текущего инструмента */
    data object CancelExecution : McpEvent

    /** Очистить историю выполнений */
    data object ClearHistory : McpEvent

    /** Переключить состояние панели (развёрнута/свёрнута) */
    data object TogglePanel : McpEvent

    /** Очистить ошибку */
    data object ClearError : McpEvent
}