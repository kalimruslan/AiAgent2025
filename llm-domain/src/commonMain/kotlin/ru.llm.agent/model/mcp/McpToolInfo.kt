package ru.llm.agent.model.mcp

import kotlinx.serialization.Serializable

/**
 * Информация об MCP инструменте для отображения в UI.
 * Упрощённая модель без привязки к конкретному LLM провайдеру.
 */
@Serializable
public data class McpToolInfo(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterInfo> = emptyMap(),
    val requiredParameters: List<String> = emptyList()
)

/**
 * Информация о параметре инструмента
 */
@Serializable
public data class ParameterInfo(
    val type: String,
    val description: String
)
