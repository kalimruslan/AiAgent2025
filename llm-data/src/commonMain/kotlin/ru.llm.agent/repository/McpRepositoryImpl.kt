package ru.llm.agent.repository

import kotlinx.serialization.json.JsonObject
import ru.llm.agent.McpClient
import ru.llm.agent.model.mcp.YaGptFunction
import ru.llm.agent.model.mcp.YaGptTool

public class McpRepositoryImpl (
    private val mcpClient: McpClient
): McpRepository {

    private var requestId = 0

    override suspend fun getMcpToolsList(): List<YaGptTool> {
        // Получаем список инструментов с MCP сервера
        val mcpTools = mcpClient.listTools()

        // Конвертируем в формат YaGPT
        return mcpTools.map { mcpTool ->
            YaGptTool(
                function = YaGptFunction(
                    name = mcpTool.name,
                    description = mcpTool.description,
                    parameters = mcpTool.inputSchema
                )
            )
        }
    }

    override suspend fun callTool(
        name: String,
        arguments: JsonObject,
    ): String {
        return mcpClient.callTool(
            name = name,
            arguments = arguments
        )
    }
}