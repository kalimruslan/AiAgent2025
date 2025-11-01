package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class ResourcesCapability(
    val subscribe: Boolean = false,
    val listChanged: Boolean = false
)