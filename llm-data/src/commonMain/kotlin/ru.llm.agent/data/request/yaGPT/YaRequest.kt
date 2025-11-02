package ru.llm.agent.data.request.yaGPT

import kotlinx.serialization.Serializable
import ru.llm.agent.model.mcp.YaGptTool

@Serializable
public data class YaRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<YaMessageRequest>,
    val tools: List<YaGptTool>? = null
)

@Serializable
public data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: Int = 2000
)