package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class McpToolsResult(
    val tools: List<McpTool>
)