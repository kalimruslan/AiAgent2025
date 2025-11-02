package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Представляет из себя инструмент
 * @param name Имя инструмента
 * @param description Описание инструмента
 * @§param inputSchema Схема входных данных
 */
@Serializable
data class McpServerTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)