
package ru.llm.agent

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Главная точка входа MCP сервера для stdio транспорта.
 * Используется для подключения к Claude Desktop и другим MCP клиентам через stdin/stdout.
 *
 * Принцип работы:
 * 1. Читает JSON-RPC запросы из stdin (по одной строке)
 * 2. Обрабатывает через McpServerSdk
 * 3. Отправляет ответы в stdout
 */
fun main() {
    // Перенаправляем stderr для логирования (stdout занят для JSON-RPC)
    System.err.println("MCP Server started (stdio mode)")

    try {
        // Создаем MCP сервер на SDK и JSON-RPC обработчик
        val mcpServerSdk = McpServerSdk()
        val jsonRpcHandler = SdkJsonRpcHandler(mcpServerSdk)

        // Конфигурируем JSON с lenient режимом для более гибкого парсинга
        val json = Json {
            prettyPrint = false  // Важно: компактный вывод для stdio
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        // Читаем из stdin
        val reader = BufferedReader(InputStreamReader(System.`in`))

        System.err.println("Waiting for requests...")

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val input = line ?: continue

            // Пропускаем пустые строки
            if (input.isBlank()) continue

            System.err.println("Received: $input")

            try {
                // Парсим JSON-RPC запрос
                val request = json.decodeFromString<JsonRpcRequest>(input)

                // Обрабатываем запрос
                val response = runBlocking {
                    jsonRpcHandler.handleRequest(request)
                }

                // Отправляем ответ в stdout
                val responseJson = json.encodeToString(
                    ru.llm.agent.data.jsonrpc.JsonRpcResponse.serializer(),
                    response
                )

                println(responseJson)
                System.out.flush()

                System.err.println("Sent: $responseJson")

            } catch (e: Exception) {
                System.err.println("Error processing request: ${e.message}")
                e.printStackTrace(System.err)

                // Отправляем ошибку обратно клиенту
                val errorResponse = ru.llm.agent.data.jsonrpc.JsonRpcResponse(
                    id = "error",
                    error = ru.llm.agent.data.jsonrpc.JsonRpcResponse.JsonRpcError(
                        code = -32700,
                        message = "Parse error: ${e.message}"
                    )
                )

                val errorJson = json.encodeToString(
                    ru.llm.agent.data.jsonrpc.JsonRpcResponse.serializer(),
                    errorResponse
                )

                println(errorJson)
                System.out.flush()
            }
        }

        System.err.println("MCP Server stopped (EOF received)")

    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace(System.err)
        exitProcess(1)
    }
}