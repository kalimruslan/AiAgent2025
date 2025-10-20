package ru.llm.agent.model

import kotlinx.serialization.Serializable

@Serializable
public data class ParseFromJsonModel(
    val answer: String,
    val tokens: String,
    val model_version: String
)