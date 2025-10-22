package ru.ai.agent.data.request.proxyapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ProxyApiRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ProxyMessageRequest>,
    @SerialName("temperature") val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    @SerialName("stream") val stream: Boolean = false
)