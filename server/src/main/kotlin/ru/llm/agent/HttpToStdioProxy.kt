package ru.llm.agent

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * HTTP-to-Stdio прокси для MCP сервера.
 *
 * Этот прокси позволяет Claude Desktop (работающему через stdio)
 * взаимодействовать с удалённым MCP сервером через HTTP.
 *
 * Принцип работы:
 * 1. Читает JSON-RPC запросы из stdin (от Claude Desktop)
 * 2. Отправляет их на удалённый HTTP MCP сервер
 * 3. Возвращает ответы в stdout (обратно Claude Desktop)
 */
class HttpToStdioProxy(
    private val remoteServerUrl: String
) {
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30_000
        }
    }

    private val json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun start() {
        System.err.println("HTTP-to-Stdio Proxy started")
        System.err.println("Remote server: $remoteServerUrl")

        BufferedReader(InputStreamReader(System.`in`)).use { reader ->
            while (true){
                val line = reader.readLine() ?: break // null = EOF
                if (line.isBlank()) continue

                try {
                    System.err.println("Received from Claude: $line")

                    // Отправляем на удалённый сервер
                    val response = client.post("$remoteServerUrl/mcp") {
                        contentType(ContentType.Application.Json)
                        setBody(line)
                    }

                    // Читаем ответ
                    val responseBody = response.bodyAsText()

                    System.err.println("Received from server: $responseBody")

                    // Отправляем обратно в stdout
                    println(responseBody)
                    System.out.flush()
                } catch (e: Exception) {
                    System.err.println("Error processing request: ${e.message}")
                    e.printStackTrace(System.err)

                    // Отправляем ошибку обратно клиенту
                    val errorResponse = JsonRpcResponse(
                        id = "error",
                        error = JsonRpcResponse.JsonRpcError(
                            code = -32603,
                            message = "Proxy error: ${e.message}"
                        )
                    )

                    val errorJson = json.encodeToString(
                        JsonRpcResponse.serializer(),
                        errorResponse
                    )

                    println(errorJson)
                    System.out.flush()
                }
            }
        }

        System.err.println("Proxy stopped")
    }
}

/**
 * Точка входа для HTTP-to-Stdio прокси
 */
fun main() {
    // Читаем URL удалённого сервера из переменной окружения
    val remoteUrl = System.getenv("REMOTE_MCP_SERVER_URL")
        ?: "https://kalimruslan-rt.ru"

    val proxy = HttpToStdioProxy(remoteUrl)

    try {
        runBlocking {
            proxy.start()
        }
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace(System.err)
        exitProcess(1)
    }
}