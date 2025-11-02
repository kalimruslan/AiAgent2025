package ru.llm.agent.compose.presenter

import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.mcp.YaGptTool

data class McpUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val mcpTools: List<YaGptTool> = emptyList(),
    val messages: List<MessageModel> = emptyList(),
    val result: String = "",
    val error: String? = null
)