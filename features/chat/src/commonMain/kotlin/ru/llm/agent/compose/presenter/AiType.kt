package ru.llm.agent.compose.presenter

sealed interface AiType {
    val displayName: String
        get() = when (this) {
            is ProxyAI -> "Proxy AI"
            is YaGptAI -> "Yandex GPT"
        }

    data class ProxyAI(
        val selectedModel: ProxyAIModel = ProxyAIModel.GPT_4O_MINI,
    ): AiType {
        enum class ProxyAIModel(val model: String) {
            CLAUDE_HAIKU("claude-3-5-haiku-20241022"),
            GPT_4O_MINI("gpt-4o-mini"), // 36.72, 146.88
            GPT_OSS("gpt-oss-20b"),
            LLAMA_8B("meta-llama/llama-3.1-8b-instruct"),
            GEMINI_FLASH_LITE("gemini-2.0-flash-lite")
        }
    }

    data class YaGptAI(
        val selectedModel: String = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
    ): AiType

    companion object {
        fun values(): List<AiType> = listOf(ProxyAI(), YaGptAI())
    }
}