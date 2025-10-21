package ru.llm.agent.data.response.yaGPT

import kotlinx.serialization.Serializable

@Serializable
public data class YandexGPTResponse(
    val result: Result
) {
    @Serializable
    public data class Result(
        val alternatives: List<Alternative>,
        val usage: Usage? = null,
        val modelVersion: String? = null
    )

    @Serializable
    public data class Alternative(
        val message: YaMessageResponse,
        val status: String? = null
    )

    @Serializable
    public data class Usage(
        val inputTextTokens: String? = null,
        val completionTokens: String? = null,
        val totalTokens: String? = null
    )
}

