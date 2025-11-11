package ru.llm.agent.model.config

import ru.llm.agent.model.LlmProvider

/**
 * Конфигурация для провайдера LLM.
 * Содержит все необходимые параметры для подключения к API провайдера.
 *
 * @param provider Тип провайдера
 * @param baseUrl Базовый URL API провайдера
 * @param modelId Идентификатор модели
 * @param displayName Отображаемое имя провайдера
 * @param defaultTemperature Температура по умолчанию
 * @param defaultMaxTokens Максимальное количество токенов по умолчанию
 */
public data class ProviderConfig(
    val provider: LlmProvider,
    val baseUrl: String,
    val modelId: String,
    val displayName: String,
    val defaultTemperature: Double,
    val defaultMaxTokens: Int
) {
    public companion object {
        /**
         * Конфигурация для YandexGPT
         */
        public fun yandexGpt(): ProviderConfig = ProviderConfig(
            provider = LlmProvider.YANDEX_GPT,
            baseUrl = "https://llm.api.cloud.yandex.net/",
            modelId = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
            displayName = "YandexGPT Lite",
            defaultTemperature = 0.6,
            defaultMaxTokens = 2000
        )

        /**
         * Конфигурация для GPT-4o-mini через ProxyAPI
         */
        public fun proxyApiGpt4oMini(): ProviderConfig = ProviderConfig(
            provider = LlmProvider.PROXY_API_GPT4O_MINI,
            baseUrl = "https://api.proxyapi.ru/openai/v1/",
            modelId = "gpt-4o-mini",
            displayName = "GPT-4o-mini",
            defaultTemperature = 0.7,
            defaultMaxTokens = 1024
        )

        /**
         * Конфигурация для Mistral AI через ProxyAPI (OpenRouter)
         */
        public fun proxyApiMistralAi(): ProviderConfig = ProviderConfig(
            provider = LlmProvider.PROXY_API_MISTRAY_AI,
            baseUrl = "https://api.proxyapi.ru/openrouter/v1/",
            modelId = "mistralai/mistral-medium-3.1",
            displayName = "Mistral AI",
            defaultTemperature = 0.7,
            defaultMaxTokens = 1024
        )

        /**
         * Получить конфигурацию для провайдера
         */
        public fun forProvider(provider: LlmProvider): ProviderConfig = when (provider) {
            LlmProvider.YANDEX_GPT -> yandexGpt()
            LlmProvider.PROXY_API_GPT4O_MINI -> proxyApiGpt4oMini()
            LlmProvider.PROXY_API_MISTRAY_AI -> proxyApiMistralAi()
        }

        /**
         * Получить все доступные конфигурации
         */
        public fun allConfigs(): List<ProviderConfig> = listOf(
            yandexGpt(),
            proxyApiGpt4oMini(),
            proxyApiMistralAi()
        )
    }
}