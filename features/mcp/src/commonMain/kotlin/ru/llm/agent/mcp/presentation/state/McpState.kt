package ru.llm.agent.mcp.presentation.state

import ru.llm.agent.mcp.model.McpToolExecutionStatus
import ru.llm.agent.model.mcp.McpToolInfo

/**
 * State для MCP feature модуля
 */
data class McpState(
    /** Список доступных MCP инструментов */
    val availableTools: List<McpToolInfo> = emptyList(),
    /** Включены ли MCP инструменты */
    val isEnabled: Boolean = false,
    /** Загружаются ли инструменты */
    val isLoadingTools: Boolean = false,
    /** Текущий выполняемый инструмент (null если не выполняется) */
    val currentExecution: McpToolExecutionStatus? = null,
    /** История выполнения инструментов */
    val executionHistory: List<McpToolExecutionStatus> = emptyList(),
    /** Ошибка загрузки инструментов */
    val error: String? = null,
    /** Развёрнута ли панель инструментов */
    val isPanelExpanded: Boolean = false
) {
    companion object {
        fun empty() = McpState()
    }
}