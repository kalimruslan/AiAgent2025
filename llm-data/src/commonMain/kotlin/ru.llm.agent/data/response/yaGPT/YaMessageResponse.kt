package ru.llm.agent.data.response.yaGPT

import kotlinx.serialization.Serializable
import ru.llm.agent.model.mcp.ToolCallList
import ru.llm.agent.model.mcp.ToolResultList

@Serializable
public data class YaMessageResponse (
    val role: String,
    val text: String? = null,
    val toolCallList: ToolCallList? = null,
    val toolResultList: ToolResultList? = null
)