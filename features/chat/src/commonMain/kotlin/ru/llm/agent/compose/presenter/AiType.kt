package ru.llm.agent.compose.presenter

sealed interface AiType {
    val displayName: String
        get() = when (this) {
            is YaGptAI -> "Yandex GPT"
        }

    data class YaGptAI(
        val selectedModel: String = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
    ): AiType

    companion object {
        fun values(): List<AiType> = listOf(YaGptAI())
    }
}