package ru.llm.agent.data.request.yandexGPT

import kotlinx.serialization.Serializable

@Serializable
public data class YaRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<YaMessageRequest>
)

@Serializable
public data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: Int = 2000
)