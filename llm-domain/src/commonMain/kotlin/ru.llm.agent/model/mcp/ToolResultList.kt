package ru.llm.agent.model.mcp

import kotlinx.serialization.Serializable

@Serializable
public class ToolResultList (
    public val toolResults: List<ToolResult>
)

@Serializable
public data class ToolResult(
    val functionResult: FunctionResult
)

@Serializable
public data class FunctionResult(
    val name: String,
    val content: String
)