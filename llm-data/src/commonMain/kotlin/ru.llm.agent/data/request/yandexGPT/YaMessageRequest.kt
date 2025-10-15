package ru.llm.agent.data.request.yandexGPT

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class YaMessageRequest (
    public val role: String,
    @SerialName("text")
    public val text: String
)