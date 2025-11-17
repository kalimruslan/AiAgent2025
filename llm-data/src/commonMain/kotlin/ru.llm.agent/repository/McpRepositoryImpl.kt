package ru.llm.agent.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.llm.agent.McpClient
import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.model.mcp.ParameterInfo
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

    override suspend fun getToolsInfo(): List<McpToolInfo> {
        val mcpTools = mcpClient.listTools()

        return mcpTools.map { mcpTool ->
            // Парсим параметры из inputSchema
            val properties = mcpTool.inputSchema["properties"]?.jsonObject ?: emptyMap()
            val required = mcpTool.inputSchema["required"]?.jsonArray
                ?.map { it.jsonPrimitive.content } ?: emptyList()

            val parameters = properties.mapValues { (_, value) ->
                val propObj = value.jsonObject
                ParameterInfo(
                    type = propObj["type"]?.jsonPrimitive?.content ?: "unknown",
                    description = propObj["description"]?.jsonPrimitive?.content ?: ""
                )
            }

            McpToolInfo(
                name = mcpTool.name,
                description = mcpTool.description,
                parameters = parameters,
                requiredParameters = required
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