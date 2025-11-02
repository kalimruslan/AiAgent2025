package ru.llm.agent.data.mcp.jsonrpc

import kotlinx.serialization.Serializable
import ru.llm.agent.data.mcp.McpToolsResult

@Serializable
public data class McpToolsListResponse(
    val jsonrpc: String,
    val result: McpToolsResult,
    val id: Int
)