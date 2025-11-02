package ru.llm.agent

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import ru.llm.agent.data.mcp.McpCallToolParams
import ru.llm.agent.data.mcp.McpCallToolRequest
import ru.llm.agent.data.mcp.McpCallToolResponse
import ru.llm.agent.data.mcp.McpTool
import ru.llm.agent.data.mcp.jsonrpc.McpRequest
import ru.llm.agent.data.mcp.jsonrpc.McpToolsListResponse
import java.util.logging.Logger

// MCP Client
public class McpClient (
    private val serverUrl: String,
    private val client: HttpClient
) {
    private var requestId = 0

    public suspend fun listTools(): List<McpTool> {
        val request = McpRequest(
            method = "tools/list",
            id = ++requestId
        )

        Logger.getLogger("McpClient").info("request tools - $request")

        val response = client.post(serverUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<McpToolsListResponse>()

        Logger.getLogger("McpClient").info("response tools: ${response}")

        return response.result.tools
    }

    public suspend fun callTool(name: String, arguments: JsonObject): String {
        val request = McpCallToolRequest(
            params = McpCallToolParams(
                name = name,
                arguments = arguments
            ),
            id = ++requestId
        )

        Logger.getLogger("McpClient").info("request call tool - $request")

        try {
            val response = client.post(serverUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<McpCallToolResponse>()

            Logger.getLogger("McpClient").info("response call tool: ${response.result}")
            return response.result.content.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Logger.getLogger("McpClient").severe("Error in callTool: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}