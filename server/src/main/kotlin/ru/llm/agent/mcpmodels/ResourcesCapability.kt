package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable

@Serializable
data class ResourcesCapability(
    val subscribe: Boolean = false,
    val listChanged: Boolean = false
)