package ru.llm.agent.model

/**
 * Информация о суммаризации истории диалога
 *
 * @param hasSummarizedMessages Есть ли в диалоге суммаризированные сообщения
 * @param summarizedMessagesCount Количество суммаризированных сообщений
 * @param savedTokens Количество токенов, сэкономленных благодаря суммаризации
 * @param lastSummarizationTimestamp Временная метка последней суммаризации (null если не было)
 */
public data class SummarizationInfo(
    val hasSummarizedMessages: Boolean = false,
    val summarizedMessagesCount: Int = 0,
    val savedTokens: Int = 0,
    val lastSummarizationTimestamp: Long? = null
) {
    public companion object {
        /**
         * Пустая информация о суммаризации (не было суммаризаций)
         */
        public fun empty(): SummarizationInfo = SummarizationInfo(
            hasSummarizedMessages = false,
            summarizedMessagesCount = 0,
            savedTokens = 0,
            lastSummarizationTimestamp = null
        )
    }
}