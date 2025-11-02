package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CallToolResult(
    val content: List<Content>
)