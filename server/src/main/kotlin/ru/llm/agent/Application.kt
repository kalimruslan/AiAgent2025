package ru.llm.agent

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, host = "localhost", port = 8080) {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello from Ktor Server!")
        }

        get("/api/data") {
            // Можно использовать код из shared модуля
            call.respond(mapOf("message" to "Data from server"))
        }
    }
}