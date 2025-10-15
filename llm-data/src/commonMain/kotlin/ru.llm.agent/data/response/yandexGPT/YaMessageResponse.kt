package ru.llm.agent.data.response.yandexGPT

import kotlinx.serialization.Serializable

@Serializable
public data class YaMessageResponse (
    val role: String,
    val text: String
)