package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class ToolsList(
    val mcpTools: List<McpTool>
)