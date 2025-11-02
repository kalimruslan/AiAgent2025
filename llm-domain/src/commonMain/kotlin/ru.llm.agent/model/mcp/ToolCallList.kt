package ru.llm.agent.model.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public class ToolCallList (
    public val toolCalls: List<ToolCall>
)

@Serializable
public data class ToolCall(
    val functionCall: FunctionCall,
    val id: String? = null
)

@Serializable
public data class FunctionCall(
    val name: String,
    val arguments: JsonObject
)