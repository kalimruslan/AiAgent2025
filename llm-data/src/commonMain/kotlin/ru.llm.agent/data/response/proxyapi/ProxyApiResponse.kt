package ru.ai.agent.data.response.proxyapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ProxyApiResponse(
    @SerialName("choices") val choices: List<ProxyChoice>,
    @SerialName("usage") val usage: ProxyUsage? = null
)

@Serializable
public data class ProxyChoice(
    @SerialName("message") val message: ProxyMessageResponse,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
public data class ProxyUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)