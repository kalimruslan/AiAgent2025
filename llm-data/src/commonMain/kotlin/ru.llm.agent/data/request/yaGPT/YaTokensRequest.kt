package ru.llm.agent.data.request.yaGPT

import kotlinx.serialization.Serializable

@Serializable
public data class YaTokensRequest(
    val modelUri: String,
    val messages: List<YaMessageRequest>
)