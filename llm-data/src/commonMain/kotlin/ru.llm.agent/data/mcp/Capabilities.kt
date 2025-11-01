package ru.llm.agent.data.mcp

import com.sun.jdi.connect.spi.TransportService
import kotlinx.serialization.Serializable

@Serializable
public data class Capabilities(
    val tools: ToolsCapability? = null,
    val resources: ResourcesCapability? = null
)