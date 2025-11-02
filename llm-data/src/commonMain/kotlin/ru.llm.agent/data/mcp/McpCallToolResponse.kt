package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class McpCallToolResponse (
    val jsonrpc: String,
    val result: McpToolResult,
    val id: Int
)