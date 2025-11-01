package ru.llm.agent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import ru.llm.agent.data.mcp.CallToolResult
import ru.llm.agent.data.mcp.InitializeResult
import ru.llm.agent.data.mcp.Tool
import ru.llm.agent.data.mcp.ToolsList
import ru.llm.agent.data.mcp.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.mcp.jsonrpc.JsonRpcResponse
import java.util.UUID
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.util.logging.Logger.getLogger

public class McpClient (private val serverUrl: String = "http://kalimruslan-rt.ru/mcp") {
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    getLogger("McpClient").info(message)
                }
            }
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }

        // Важно: не выбрасывать исключение на не-2xx статусы
        expectSuccess = false
    }

    // Инициализация
    public suspend fun initialize(): Result<InitializeResult> = withContext(Dispatchers.IO) {
        try {
            getLogger("McpClient").info( "Connecting to: $serverUrl")

            val request = JsonRpcRequest(
                id = generateId(),
                method = "initialize",
                params = buildJsonObject { }
            )

            getLogger("McpClient").info("Request: ${json.encodeToString(JsonRpcRequest.serializer(), request)}")

            val httpResponse: HttpResponse = client.post(serverUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            getLogger("McpClient").info("Response status: ${httpResponse.status}")
            getLogger("McpClient").info("Response body: ${httpResponse.bodyAsText()}")

            if (!httpResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    Exception("Server error: ${httpResponse.status} - ${httpResponse.bodyAsText()}")
                )
            }

            val response = json.decodeFromString<JsonRpcResponse>(httpResponse.bodyAsText())

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val result = Json.decodeFromJsonElement<InitializeResult>(response.result!!)
                Result.success(result)
            }
        } catch (e: Exception) {
            getLogger("McpClient").info("Initialize error - $e")
            Result.failure(e)
        }
    }

    // Получить список инструментов
    public suspend fun listTools(): Result<List<Tool>> = withContext(Dispatchers.IO) {
        try {
            val request = JsonRpcRequest(
                id = generateId(),
                method = "tools/list"
            )

            val httpResponse: HttpResponse = client.post(serverUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    Exception("Server error: ${httpResponse.status}")
                )
            }

            val response = json.decodeFromString<JsonRpcResponse>(httpResponse.bodyAsText())

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val result = Json.decodeFromJsonElement<ToolsList>(response.result!!)
                Result.success(result.tools)
            }
        } catch (e: Exception) {
            getLogger("McpClient").info("List tools error -$e")
            Result.failure(e)
        }
    }

    // Вызвать инструмент
    public suspend fun callTool(
        toolName: String,
        arguments: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("name", toolName)
                putJsonObject("arguments") {
                    arguments.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value.toDouble())
                            is Boolean -> put(key, value)
                        }
                    }
                }
            }

            val request = JsonRpcRequest(
                id = generateId(),
                method = "tools/call",
                params = params
            )

            val httpResponse: HttpResponse = client.post(serverUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    Exception("Server error: ${httpResponse.status}")
                )
            }

            val response = json.decodeFromString<JsonRpcResponse>(httpResponse.bodyAsText())

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val result = Json.decodeFromJsonElement<CallToolResult>(response.result!!)
                val text = result.content.firstOrNull()?.text ?: ""
                Result.success(text)
            }
        } catch (e: Exception) {
            getLogger("McpClient").info("Call tool error - $e")
            Result.failure(e)
        }
    }

    // Вспомогательные методы для конкретных инструментов
    public suspend fun echo(text: String): Result<String> {
        return callTool("echo", mapOf("text" to text))
    }

    public suspend fun add(a: Number, b: Number): Result<String> {
        return callTool("add", mapOf("a" to a, "b" to b))
    }

    public suspend fun getCurrentTime(): Result<String> {
        return callTool("getCurrentTime", emptyMap())
    }

    public fun close() {
        client.close()
    }

    private fun generateId(): String = UUID.randomUUID().toString()

}