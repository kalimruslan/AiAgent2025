package ru.llm.agent.data.response

import kotlinx.serialization.Serializable

@Serializable
public data class YandexGPTResponse(
    val result: Result
) {
    @Serializable
    public data class Result(
        val alternatives: List<Alternative>,
        val usage: Usage,
        val modelVersion: String
    )

    @Serializable
    public data class Alternative(
        val message: YaMessageResponse,
        val status: String
    )

    @Serializable
    public data class Usage(
        val inputTextTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )
}

