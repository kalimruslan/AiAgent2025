package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolsList(
    val tools: List<Tool>
)