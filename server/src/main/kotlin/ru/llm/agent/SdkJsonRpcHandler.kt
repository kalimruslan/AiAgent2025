package ru.llm.agent

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Обработчик JSON-RPC запросов для MCP SDK сервера.
 * Преобразует HTTP JSON-RPC в вызовы SDK и обратно.
 */
class SdkJsonRpcHandler(
    private val mcpServer: McpServerSdk
) {

    /**
     * Обрабатывает JSON-RPC запрос и возвращает ответ
     */
    fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return try {
            when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleToolsList(request)
                "tools/call" -> handleToolsCall(request)
                else -> createErrorResponse(
                    request.id,
                    -32601,
                    "Method not found: ${request.method}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createErrorResponse(request.id, -32603, "Internal error: ${e.message}")
        }
    }

    /**
     * Обрабатывает запрос инициализации
     */
    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        val result = buildJsonObject {
            putJsonObject("serverInfo") {
                put("name", "llm-agent-mcp-server")
                put("version", "1.0.0")
            }
            putJsonObject("capabilities") {
                putJsonObject("tools") {
                    put("listChanged", false)
                }
            }
        }

        return JsonRpcResponse(id = request.id, result = result)
    }

    /**
     * Обрабатывает запрос списка инструментов
     */
    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        val tools = mcpServer.getToolsList()

        val result = buildJsonObject {
            putJsonArray("tools") {
                tools.forEach { tool ->
                    add(buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        // Преобразуем Tool.Input в JSON schema
                        putJsonObject("inputSchema") {
                            put("type", "object")
                            put("properties", tool.inputSchema.properties)
                            tool.inputSchema.required?.let { req ->
                                putJsonArray("required") {
                                    req.forEach { add(JsonPrimitive(it)) }
                                }
                            }
                        }
                    })
                }
            }
        }

        return JsonRpcResponse(id = request.id, result = result)
    }

    /**
     * Обрабатывает вызов инструмента
     */
    private fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
        if (request.params == null) {
            return createErrorResponse(request.id, -32602, "Missing params")
        }

        val toolName = request.params["name"]?.jsonPrimitive?.content
            ?: return createErrorResponse(request.id, -32602, "Missing tool name")

        val arguments = request.params["arguments"]?.jsonObject
            ?: buildJsonObject {}

        return try {
            // Вызываем tool через SDK
            val callToolResult = runBlocking {
                mcpServer.callTool(toolName, arguments)
            }

            // Преобразуем результат SDK в JSON-RPC формат
            val result = buildJsonObject {
                putJsonArray("content") {
                    callToolResult.content.forEach { content ->
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", (content as? TextContent)?.text ?: "")
                        })
                    }
                }
            }

            JsonRpcResponse(id = request.id, result = result)
        } catch (e: Exception) {
            e.printStackTrace()
            createErrorResponse(request.id, -32603, "Tool execution error: ${e.message}")
        }
    }

    /**
     * Создает ответ с ошибкой
     */
    private fun createErrorResponse(id: String, code: Int, message: String): JsonRpcResponse {
        return JsonRpcResponse(
            id = id,
            error = JsonRpcResponse.JsonRpcError(code = code, message = message)
        )
    }
}