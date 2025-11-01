package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable

@Serializable
data class InitializeResult(
    val protocolVersion: String = "2024-11-05",
    val serverInfo: ServerInfo,
    val capabilities: Capabilities
)