package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)