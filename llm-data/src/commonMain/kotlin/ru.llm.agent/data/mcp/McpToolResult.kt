package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class McpToolResult(
    val content: List<McpContent>
)