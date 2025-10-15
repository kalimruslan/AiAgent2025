package ru.llm.agent.api

import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import ru.llm.agent.data.request.YaRequest

public class YandexApi internal constructor(
    private val httpClient: HttpClient,
) {
    public suspend fun sendMessage(request: YaRequest): HttpResponse {
        return httpClient.request("foundationModels/v1/completion") {
            method = HttpMethod.Post
            setBody(request)
        }
    }
}