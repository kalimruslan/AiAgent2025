package ru.llm.agent.usecase.config

import ru.llm.agent.core.utils.model.TokenCount

/**
 * Конфигурация для суммаризации истории диалога.
 * Определяет параметры сжатия контекста при приближении к лимиту токенов.
 *
 * @param tokenThreshold Порог использования токенов для запуска суммаризации (0.0 - 1.0)
 * @param keepLastMessages Сколько последних сообщений НЕ суммаризировать (сохраняются для контекста)
 * @param summarizationMaxTokens Максимальное количество токенов для текста суммаризации
 * @param minMessagesToSummarize Минимальное количество сообщений для суммаризации
 */
public data class SummarizationConfig(
    val tokenThreshold: Double = 0.75,
    val keepLastMessages: Int = 3,
    val summarizationMaxTokens: TokenCount = TokenCount(300),
    val minMessagesToSummarize: Int = 5
) {
    init {
        require(tokenThreshold in 0.0..1.0) {
            "tokenThreshold должен быть в диапазоне от 0.0 до 1.0, получено: $tokenThreshold"
        }
        require(keepLastMessages >= 0) {
            "keepLastMessages не может быть отрицательным, получено: $keepLastMessages"
        }
        require(minMessagesToSummarize > 0) {
            "minMessagesToSummarize должен быть больше 0, получено: $minMessagesToSummarize"
        }
    }

    /**
     * Проверяет, нужна ли суммаризация при текущем состоянии токенов
     *
     * @param currentTokens Текущее количество использованных токенов
     * @param maxTokens Максимальное количество токенов
     * @return true если суммаризация необходима
     */
    public fun shouldSummarize(currentTokens: TokenCount, maxTokens: TokenCount): Boolean {
        val usageRatio = currentTokens.value.toDouble() / maxTokens.value.toDouble()
        return usageRatio >= tokenThreshold
    }

    public companion object {
        /**
         * Конфигурация по умолчанию
         */
        public val DEFAULT: SummarizationConfig = SummarizationConfig()

        /**
         * Агрессивная конфигурация (суммаризация при 50% использования)
         */
        public val AGGRESSIVE: SummarizationConfig = SummarizationConfig(
            tokenThreshold = 0.5,
            keepLastMessages = 2,
            summarizationMaxTokens = TokenCount(200),
            minMessagesToSummarize = 3
        )

        /**
         * Консервативная конфигурация (суммаризация при 90% использования)
         */
        public val CONSERVATIVE: SummarizationConfig = SummarizationConfig(
            tokenThreshold = 0.9,
            keepLastMessages = 5,
            summarizationMaxTokens = TokenCount(400),
            minMessagesToSummarize = 7
        )
    }
}