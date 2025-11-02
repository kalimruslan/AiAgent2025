package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class ServerInfo(
    val name: String,
    val version: String
)