/*
package ru.llm.agent.data

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import kotlin.time.Duration.Companion.seconds

*/
/**
 * Этот класс McpServer реализует WebSocket-сервер,
 * который обрабатывает запросы по MCP (Model Control Protocol) — протоколу,
 * используемому для взаимодействия с моделями искусственного интеллекта.
 * Он работает на основе Ktor и JSON-RPC.
 *//*

class McpServerOld {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    */
/**
     * Конфигурирует сервер Ktor, устанавливает вебсокет /mcp и логику обработки сообщений.
     * Устанавливает параметры вебсокета: пинг, таймаут, размер фрейма.
     * Регистрирует обработчик для /mcp.
     * При подключении клиента выводит сообщение.
     * В цикле считывает входящие фреймы, обрабатывает текстовые сообщения.
     * Вызывает handleRequest() для ответа.
     * Ловит ошибки и отслеживает отключение клиента.
     *//*

    fun Application.configureMcp() {
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/mcp") {
                println("Client connected")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            println("Received: $text")

                            val request = json.decodeFromString<JsonRpcRequest>(text)
                            val response = handleRequest(request)

                            val responseText = json.encodeToString(value = response)
                            println("Sending: $responseText")
                            send(Frame.Text(responseText))
                        }
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    e.printStackTrace()
                } finally {
                    println("Client disconnected")
                }
            }
        }
    }

    */
/**
     * Обрабатывает JSON-RPC-запрос в зависимости от метода.
     *
     * Определяет метод (request.method) и вызывает соответствующую функцию.
     * В случае ошибки возвращает JsonRpcError.
     *//*

    private fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return try {
            when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleToolsList(request)
                "tools/call" -> handleToolCall(request)
                "ping" -> handlePing(request)
                else -> JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcResponse.JsonRpcError(-32601, "Method not found")
                )
            }
        } catch (e: Exception) {
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcResponse.JsonRpcError(-32603, "Internal error: ${e.message}")
            )
        }
    }

    */
/**
     * Отвечает на метод initialize, возвращая информацию о сервере и его возможностях.
     *
     * Возвращает protocolVersion, serverInfo и capabilities.
     * Используется при первом подключении клиента.
     *//*

    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        val result = JsonRpcResponse.InitializeResult(
            protocolVersion = "2024-11-05",
            serverInfo = JsonRpcResponse.ServerInfo(
                name = "Simple MCP Server",
                version = "1.0.0"
            ),
            capabilities = JsonRpcResponse.ServerCapabilities(
                tools = JsonRpcResponse.ToolsCapability(listChanged = true)
            )
        )

        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    */
/**
     * Возвращает список доступных "инструментов" (tool), которые может использовать клиент.
     *
     * Пример инструментов: get_time, echo.
     * Каждый инструмент имеет имя, описание и схему входных данных.
     *//*

    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        val tools = JsonRpcResponse.ToolsList(
            tools = listOf(
                JsonRpcResponse.Tool(
                    name = "get_time",
                    description = "Получить текущее время",
                    inputSchema = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {})
                    }
                ),
                JsonRpcResponse.Tool(
                    name = "echo",
                    description = "Повторить сообщение",
                    inputSchema = buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("message", buildJsonObject {
                                put("type", "string")
                                put("description", "Сообщение для повтора")
                            })
                        })
                        put("required", buildJsonArray { add("message") })
                    }
                )
            )
        )

        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(tools)
        )
    }

    */
/**
     * Выполняет вызов конкретного инструмента (tool call) по имени.
     *
     * Получает аргументы из request.params.
     * Выполняет логику инструмента (например, get_time, echo).
     * Возвращает результат как JSON-объект.
     *
     *//*

    private fun handleToolCall(request: JsonRpcRequest): JsonRpcResponse {
        val params = request.params ?: return JsonRpcResponse(
            id = request.id,
            error = JsonRpcResponse.JsonRpcError(-32602, "Invalid params")
        )

        val toolName = params["name"]?.jsonPrimitive?.content
        val arguments = params["arguments"]?.jsonObject

        val resultText = when (toolName) {
            "get_time" -> "Текущее время: ${System.currentTimeMillis()}"
            "echo" -> {
                val message = arguments?.get("message")?.jsonPrimitive?.content
                "Эхо: $message"
            }

            else -> "Неизвестный инструмент: $toolName"
        }

        val result = JsonRpcResponse.CallToolResult(
            content = listOf(JsonRpcResponse.TextContent(text = resultText))
        )

        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    */
/**
     * Обрабатывает метод ping, возвращая ответ pong.
     *
     * Обрабатывает метод ping, возвращая ответ pong. Используется для проверки работоспособности соединения.
     *//*

    private fun handlePing(request: JsonRpcRequest): JsonRpcResponse {
        return JsonRpcResponse(
            id = request.id,
            result = buildJsonObject { put("status", "pong") }
        )
    }
}*/
