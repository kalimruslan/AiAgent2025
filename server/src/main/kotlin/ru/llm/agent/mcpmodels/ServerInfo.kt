package ru.llm.agent.mcpmodels

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)