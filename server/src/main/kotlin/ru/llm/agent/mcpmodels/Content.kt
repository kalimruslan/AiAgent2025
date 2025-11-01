package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Content(
    val type: String = "text",
    val text: String
)