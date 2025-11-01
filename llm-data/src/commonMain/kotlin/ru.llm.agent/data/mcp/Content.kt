package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class Content(
    val type: String = "text",
    val text: String
)