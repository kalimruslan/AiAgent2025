package ru.llm.agent.model.mcp

/**
 * Domain модель MCP сервера (удаленного или локального)
 */
public data class McpServer(
    val id: Long = 0,
    val name: String,
    val type: McpServerType = McpServerType.REMOTE,

    // Для удаленных серверов
    val url: String? = null,

    // Для локальных серверов
    val command: String? = null,
    val args: List<String>? = null,
    val env: Map<String, String>? = null,

    val description: String? = null,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        when (type) {
            McpServerType.REMOTE -> require(url != null) {
                "URL обязателен для удаленного сервера"
            }
            McpServerType.LOCAL -> require(command != null) {
                "Command обязателен для локального сервера"
            }
        }
    }
}