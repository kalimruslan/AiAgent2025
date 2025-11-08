package ru.llm.agent.model

/**
 * Мнение эксперта по заданному вопросу
 */
public data class ExpertOpinion(
    /** ID мнения */
    public val id: Long = 0,
    /** ID эксперта */
    public val expertId: String,
    /** Название эксперта */
    public val expertName: String,
    /** Иконка эксперта */
    public val expertIcon: String,
    /** ID сообщения, к которому относится мнение */
    public val messageId: Long,
    /** Текст мнения эксперта */
    public val opinion: String,
    /** Временная метка */
    public val timestamp: Long,
    /** Оригинальный JSON ответ */
    public val originalResponse: String? = null
)
