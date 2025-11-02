package ru.llm.agent.repository

import kotlinx.serialization.json.JsonObject
import ru.llm.agent.model.mcp.YaGptTool

public interface McpRepository{
    public suspend fun getMcpToolsList(): List<YaGptTool>

    public suspend fun callTool(name: String, arguments: JsonObject): String
}