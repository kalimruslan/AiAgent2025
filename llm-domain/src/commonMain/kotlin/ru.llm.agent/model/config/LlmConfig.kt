package ru.llm.agent.model.config

import ru.llm.agent.model.LlmProvider

/**
 * Конфигурация для LLM модели.
 * Определяет все параметры, необходимые для работы с языковой моделью.
 *
 * @param modelId Идентификатор модели (например, "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite")
 * @param temperature Температура генерации (0.0 - детерминированный, 1.0 - креативный)
 * @param maxTokens Максимальное количество токенов в ответе
 * @param systemPrompt Системный промпт (опционально, можно переопределять для каждого запроса)
 * @param provider Провайдер LLM
 * @param stream Использовать ли потоковую передачу ответа
 */
public data class LlmConfig(
    val modelId: String,
    val temperature: Double,
    val maxTokens: Int,
    val systemPrompt: String? = null,
    val provider: LlmProvider,
    val stream: Boolean = false
) {
    /**
     * Создать копию конфигурации с изменением системного промпта
     */
    public fun withSystemPrompt(prompt: String): LlmConfig = copy(systemPrompt = prompt)

    /**
     * Создать копию конфигурации с изменением температуры
     */
    public fun withTemperature(temp: Double): LlmConfig = copy(temperature = temp)

    /**
     * Создать копию конфигурации с изменением максимального количества токенов
     */
    public fun withMaxTokens(tokens: Int): LlmConfig = copy(maxTokens = tokens)

    public companion object {
        /**
         * Конфигурация по умолчанию для YandexGPT
         */
        public fun defaultYandexGpt(): LlmConfig = LlmConfig(
            modelId = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
            temperature = 0.6,
            maxTokens = 2000,
            provider = LlmProvider.YANDEX_GPT,
            stream = false
        )

        /**
         * Конфигурация для суммаризации текста (YandexGPT)
         */
        public fun summarizationYandexGpt(): LlmConfig = LlmConfig(
            modelId = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
            temperature = 0.3,
            maxTokens = 500,
            systemPrompt = "Ты помощник, который кратко суммирует текст, сохраняя ключевую информацию.",
            provider = LlmProvider.YANDEX_GPT,
            stream = false
        )

        /**
         * Конфигурация для аналитики цепочки агентов (YandexGPT)
         */
        public fun chainAnalysisYandexGpt(): LlmConfig = LlmConfig(
            modelId = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
            temperature = 0.3,
            maxTokens = 500,
            provider = LlmProvider.YANDEX_GPT,
            stream = false
        )

        /**
         * Конфигурация по умолчанию для ProxyAPI GPT-4o-mini
         */
        public fun defaultProxyApiGpt4o(): LlmConfig = LlmConfig(
            modelId = "gpt-4o-mini",
            temperature = 0.7,
            maxTokens = 1024,
            provider = LlmProvider.PROXY_API_GPT4O_MINI,
            stream = false
        )

        /**
         * Конфигурация по умолчанию для ProxyAPI Mistral AI
         */
        public fun defaultProxyApiMistral(): LlmConfig = LlmConfig(
            modelId = "mistralai/mistral-medium-3.1",
            temperature = 0.7,
            maxTokens = 1024,
            provider = LlmProvider.PROXY_API_MISTRAY_AI,
            stream = false
        )

        /**
         * Конфигурация для критических задач (низкая температура)
         */
        public fun preciseLlm(provider: LlmProvider): LlmConfig = when (provider) {
            LlmProvider.YANDEX_GPT -> defaultYandexGpt().withTemperature(0.3)
            LlmProvider.PROXY_API_GPT4O_MINI -> defaultProxyApiGpt4o().withTemperature(0.3)
            LlmProvider.PROXY_API_MISTRAY_AI -> defaultProxyApiMistral().withTemperature(0.3)
        }

        /**
         * Конфигурация для креативных задач (высокая температура)
         */
        public fun creativeLlm(provider: LlmProvider): LlmConfig = when (provider) {
            LlmProvider.YANDEX_GPT -> defaultYandexGpt().withTemperature(0.9)
            LlmProvider.PROXY_API_GPT4O_MINI -> defaultProxyApiGpt4o().withTemperature(0.9)
            LlmProvider.PROXY_API_MISTRAY_AI -> defaultProxyApiMistral().withTemperature(0.9)
        }
    }
}

/**
 * Тип задачи для автоматического выбора конфигурации
 */
public enum class LlmTaskType {
    /** Обычный диалог */
    CONVERSATION,

    /** Суммаризация текста */
    SUMMARIZATION,

    /** Анализ и проверка */
    ANALYSIS,

    /** Креативная генерация */
    CREATIVE,

    /** Точные вычисления/логика */
    PRECISE
}

/**
 * Extension для получения подходящей конфигурации по типу задачи
 */
public fun LlmTaskType.getConfig(provider: LlmProvider): LlmConfig = when (this) {
    LlmTaskType.CONVERSATION -> when (provider) {
        LlmProvider.YANDEX_GPT -> LlmConfig.defaultYandexGpt()
        LlmProvider.PROXY_API_GPT4O_MINI -> LlmConfig.defaultProxyApiGpt4o()
        LlmProvider.PROXY_API_MISTRAY_AI -> LlmConfig.defaultProxyApiMistral()
    }
    LlmTaskType.SUMMARIZATION -> LlmConfig.summarizationYandexGpt()
    LlmTaskType.ANALYSIS -> LlmConfig.chainAnalysisYandexGpt()
    LlmTaskType.CREATIVE -> LlmConfig.creativeLlm(provider)
    LlmTaskType.PRECISE -> LlmConfig.preciseLlm(provider)
}