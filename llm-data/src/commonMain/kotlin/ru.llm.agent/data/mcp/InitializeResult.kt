package ru.llm.agent.data.mcp

import kotlinx.serialization.Serializable

@Serializable
public data class InitializeResult(
    val protocolVersion: String = "2024-11-05",
    val serverInfo: ServerInfo,
    val capabilities: Capabilities
)