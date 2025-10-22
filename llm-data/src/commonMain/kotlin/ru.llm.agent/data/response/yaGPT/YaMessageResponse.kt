package ru.llm.agent.data.response.yaGPT

import kotlinx.serialization.Serializable

@Serializable
public data class YaMessageResponse (
    val role: String,
    val text: String
)