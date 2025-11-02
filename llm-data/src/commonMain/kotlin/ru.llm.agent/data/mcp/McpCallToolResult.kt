package ru.llm.agent.data.mcp

public data class McpCallToolResult (
    val jsonrpc: String = "2.0",
    val method: String = "tools/call",
    val params: McpCallToolParams,
    val id: Int
)