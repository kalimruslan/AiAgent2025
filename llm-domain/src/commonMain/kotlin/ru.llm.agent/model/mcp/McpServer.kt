package ru.llm.agent.model.mcp

/**
 * Domain модель удаленного MCP сервера
 */
public data class McpServer(
    val id: Long = 0,
    val name: String,
    val url: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)