package ru.llm.agent.model

/**
 * Провайдеры LLM для выбора модели
 */
public enum class LlmProvider(
    public val displayName: String,
    public val modelId: String
) {
    /**
     * Yandex GPT Lite
     */
    YANDEX_GPT(
        displayName = "YandexGPT Lite",
        modelId = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite"
    ),

    /**
     * OpenAI GPT-4o-mini через ProxyAPI
     */
    PROXY_API_GPT4O_MINI(
        displayName = "GPT-4o-mini",
        modelId = "gpt-4o-mini"
    );

    public companion object {
        /**
         * Получить провайдер по его modelId
         */
        public fun fromModelId(modelId: String): LlmProvider? {
            return entries.find { it.modelId == modelId }
        }

        /**
         * Провайдер по умолчанию
         */
        public fun default(): LlmProvider = YANDEX_GPT
    }
}
