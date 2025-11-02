package ru.llm.agent.model.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class YaGptTool(
    val function: YaGptFunction
)

@Serializable
public data class YaGptFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)