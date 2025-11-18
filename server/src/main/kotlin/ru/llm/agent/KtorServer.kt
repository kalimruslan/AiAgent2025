package ru.llm.agent

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse

/**
 * Главная точка входа MCP сервера.
 * Использует Ktor для HTTP транспорта и официальный MCP Kotlin SDK для обработки протокола.
 */
fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        // Создаем MCP сервер на SDK и JSON-RPC обработчик
        val mcpServerSdk = McpServerSdk()
        val jsonRpcHandler = SdkJsonRpcHandler(mcpServerSdk)

        routing {
            get("/") {
                call.respondText("MCP Server is running (powered by Kotlin SDK)", ContentType.Text.Plain)
            }

            post("/mcp") {
                try {
                    val request = call.receive<JsonRpcRequest>()
                    val response = jsonRpcHandler.handleRequest(request)
                    call.respond(response)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        JsonRpcResponse(
                            id = "error",
                            error = JsonRpcResponse.JsonRpcError(
                                -32700,
                                "Parse error: ${e.message}"
                            )
                        )
                    )
                }
            }
        }
    }.start(wait = true)
}
