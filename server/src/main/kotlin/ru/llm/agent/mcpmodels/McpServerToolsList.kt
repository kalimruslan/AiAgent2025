package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable

@Serializable
data class McpServerToolsList(
    val tools: List<McpServerTool>
)