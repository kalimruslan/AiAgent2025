package ru.llm.agent.model

/**
 * Режим работы диалога
 */
public enum class ConversationMode(
    public val displayName: String,
    public val description: String
) {
    /**
     * Обычный режим - один AI ассистент отвечает на вопросы
     */
    SINGLE(
        displayName = "Single AI",
        description = "Один AI консультант"
    ),

    /**
     * Режим комитета экспертов - несколько AI с разными ролями дают свои мнения
     */
    COMMITTEE(
        displayName = "Committee",
        description = "Комитет экспертов"
    );

    public companion object {
        /**
         * Режим по умолчанию
         */
        public fun default(): ConversationMode = SINGLE
    }
}
