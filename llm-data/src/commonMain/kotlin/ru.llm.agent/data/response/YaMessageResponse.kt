package ru.llm.agent.data.response

import kotlinx.serialization.Serializable

@Serializable
public data class YaMessageResponse (
    val role: String,
    val text: String
)