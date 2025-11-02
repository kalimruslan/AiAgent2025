package ru.llm.agent.mcpmodels

import com.sun.jdi.connect.spi.TransportService
import kotlinx.serialization.Serializable

@Serializable
data class Capabilities(
    val tools: ToolsCapability? = null,
    val resources: ResourcesCapability? = null
)