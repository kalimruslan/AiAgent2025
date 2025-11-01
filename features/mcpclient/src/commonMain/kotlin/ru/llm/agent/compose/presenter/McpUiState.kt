package ru.llm.agent.compose.presenter

import ru.llm.agent.data.mcp.Tool

data class McpUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val tools: List<Tool> = emptyList(),
    val result: String = "",
    val error: String? = null
)