package ru.llm.agent.data.mcp

import com.sun.jdi.connect.spi.TransportService
import kotlinx.serialization.Serializable

@Serializable
public data class ToolsCapability(
    val listChanged: Boolean = false
)