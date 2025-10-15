package ru.llm.agent.data.request.proxyapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class ProxyMessageRequest (
    public val role: String,
    @SerialName("content")
    public val text: String
)