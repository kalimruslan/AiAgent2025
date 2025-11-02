package ru.llm.agent.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse
import ru.llm.agent.mcpmodels.CallToolResult
import ru.llm.agent.mcpmodels.Capabilities
import ru.llm.agent.mcpmodels.Content
import ru.llm.agent.mcpmodels.InitializeResult
import ru.llm.agent.mcpmodels.ServerInfo
import ru.llm.agent.mcpmodels.McpServerTool
import ru.llm.agent.mcpmodels.ToolsCapability
import ru.llm.agent.mcpmodels.McpServerToolsList
import kotlin.text.get

class McpServer {
    private val mcpServerTools = listOf(
        McpServerTool(
            name = "echo",
            description = "Возвращает введенный текст",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Текст для echo")
                    }
                }
                putJsonArray("required") {
                    add("text")
                }
            }
        ),
        McpServerTool(
            name = "add",
            description = "Складывает два числа",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Введите выражение")
                    }
                }
                putJsonArray("required") {
                    add("text")
                }
            }
        ),
        McpServerTool(
            name = "getCurrentTime",
            description = "Возвращает текущее время",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {}
            }
        )
    )

    fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return try {
            val result = when (request.method) {
                "initialize" -> handleInitialize()
                "tools/list" -> handleToolsList()
                "tools/call" -> handleToolsCall(request.params)
                else -> return createErrorResponse(
                    request.id,
                    -32601,
                    "Method not found: ${request.method}"
                )
            }
            JsonRpcResponse(id = request.id, result = result)
        } catch (e: Exception) {
            createErrorResponse(request.id, -32603, "Internal error: ${e.message}")
        }
    }

    private fun handleInitialize(): JsonElement {
        val result = InitializeResult(
            serverInfo = ServerInfo(
                name = "Simple MCP Server",
                version = "1.0.0"
            ),
            capabilities = Capabilities(
                tools = ToolsCapability(listChanged = false)
            )
        )
        return Json.encodeToJsonElement(result)
    }

    private fun handleToolsList(): JsonElement {
        val result = McpServerToolsList(tools = mcpServerTools)
        return Json.encodeToJsonElement(result)
    }

    private fun handleToolsCall(params: JsonObject?): JsonElement {
        if (params == null) {
            throw IllegalArgumentException("Missing params")
        }

        val toolName = params["name"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing tool name")

        val arguments = params["arguments"]?.jsonObject
            ?: JsonObject(emptyMap())

        val resultText = when (toolName) {
            "echo" -> {
                val text = arguments["text"]?.jsonPrimitive?.content ?: ""
                "Echo: $text"
            }
            "add" -> {
                val text = arguments["text"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing 'text' in arguments")

                val cleanText = text.replace(" ", "")
                val parts = cleanText.split("+")

                if (parts.size != 2) {
                    throw IllegalArgumentException("Invalid format. Expected 'a + b', got: $text")
                }

                val a = parts[0].toDoubleOrNull()
                val b = parts[1].toDoubleOrNull()

                if (a == null || b == null) {
                    throw IllegalArgumentException("Invalid numbers: $text")
                }

                "Result: ${a + b}"
            }
            "getCurrentTime" -> {
                "Current time: ${System.currentTimeMillis()}"
            }
            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }

        val result = CallToolResult(
            content = listOf(Content(text = resultText))
        )
        return Json.encodeToJsonElement(result)
    }

    private fun createErrorResponse(id: String, code: Int, message: String): JsonRpcResponse {
        return JsonRpcResponse(
            id = id,
            error = JsonRpcResponse.JsonRpcError(code = code, message = message)
        )
    }
}