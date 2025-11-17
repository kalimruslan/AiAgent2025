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
import ru.llm.agent.data.McpServer
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse

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

        val mcpServer = McpServer()

        routing {
            get("/") {
                call.respondText("MCP Server is running", ContentType.Text.Plain)
            }

            post("/mcp") {
                try {
                    val request = call.receive<JsonRpcRequest>()
                    val response = mcpServer.handleRequest(request)
                    call.respond(response)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
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
