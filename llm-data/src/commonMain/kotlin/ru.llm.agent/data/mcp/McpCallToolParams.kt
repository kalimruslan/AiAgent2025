package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class McpCallToolParams (
    val name: String,
    val arguments: JsonObject
)