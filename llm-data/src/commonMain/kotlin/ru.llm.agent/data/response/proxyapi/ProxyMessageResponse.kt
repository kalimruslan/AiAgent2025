package ru.llm.agent.data.response.proxyapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ProxyMessageResponse (
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)