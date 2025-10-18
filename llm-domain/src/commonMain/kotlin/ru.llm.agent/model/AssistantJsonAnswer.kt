package ru.llm.agent.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AssistantJsonAnswer(
    @SerialName("answer")
    val answer: String? = null,
    @SerialName("is_continue")
    val isCOntinue: Boolean? = null,
    @SerialName("is_complete")
    val isComplete: Boolean? = null
)