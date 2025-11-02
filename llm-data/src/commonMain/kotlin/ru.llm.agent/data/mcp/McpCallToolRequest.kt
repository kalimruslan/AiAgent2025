package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class McpCallToolRequest (
    val jsonrpc: String = "2.0",
    val method: String = "tools/call",
    val params: McpCallToolParams,
    val id: Int
)