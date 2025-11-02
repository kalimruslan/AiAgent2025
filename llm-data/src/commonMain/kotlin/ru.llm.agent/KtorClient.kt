package ru.llm.agent

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json

public fun createHttpClient(
    developerToken: String,
    baseUrl: String,
    json: Json = DefaultJson,
): HttpClient {
    val httpClient = frameHttpClient(baseUrl, developerToken, json)
    return httpClient
}

private fun frameHttpClient(
    baseUrl: String,
    developerToken: String,
    json: Json? = null,
): HttpClient {
    return HttpClient {
        if (json != null) {
            install(ContentNegotiation) {
                json(jsonForFrame(json))
            }
        }

        install(Logging){
            logger = Logger.DEFAULT
            level = LogLevel.ALL

            // Кастомный логгер
            logger = object : Logger {
                override fun log(message: String) {
                    println("🌐 KTOR: $message")
                }
            }
        }

        defaultRequest {
            url {
                takeFrom(baseUrl)
            }
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $developerToken")
            if(baseUrl.contains("yandex.net")){
                header("x-folder-id", "b1gonedr4v7ke927m32n")
            }
        }
    }
}

private fun jsonForFrame(baseJson: Json): Json {
    return Json(from = baseJson) {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
    }
}
