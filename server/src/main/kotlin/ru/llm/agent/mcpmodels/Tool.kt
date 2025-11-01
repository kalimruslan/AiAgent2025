package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)