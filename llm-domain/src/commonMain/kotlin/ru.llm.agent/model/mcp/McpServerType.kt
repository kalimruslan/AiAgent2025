package ru.llm.agent.model.mcp

/**
 * Тип MCP сервера
 */
public enum class McpServerType {
    /** Удаленный сервер через HTTP */
    REMOTE,

    /** Локальный сервер через stdio (запускается как процесс) */
    LOCAL
}