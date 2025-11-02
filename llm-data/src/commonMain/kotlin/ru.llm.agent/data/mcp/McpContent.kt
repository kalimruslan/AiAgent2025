package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class McpContent(
    val type: String,
    val text: String
)