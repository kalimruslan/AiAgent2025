package ru.llm.agent.api

import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import ru.ai.agent.data.request.proxyapi.ProxyApiRequest

public class ProxyApi internal constructor(
    private val httpClientOpenAi: HttpClient,
    private val httpClientOpenRouter: HttpClient,
) {
    public suspend fun sendMessage(request: ProxyApiRequest): HttpResponse {
        val httpClient= if(request.model.startsWith("gpt")) httpClientOpenAi else httpClientOpenRouter
        return  httpClient.request("chat/completions") {
            method = HttpMethod.Post
            setBody(request)
        }
    }
}