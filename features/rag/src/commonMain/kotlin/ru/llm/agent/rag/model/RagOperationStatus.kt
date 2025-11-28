package ru.llm.agent.rag.model

/**
 * Статус выполнения операции RAG
 */
data class RagOperationStatus(
    /** Тип операции (indexing, clearing, searching) */
    val operationType: String,
    /** Описание выполняемой операции */
    val description: String,
    /** Флаг выполнения (анимация индикатора) */
    val isExecuting: Boolean = true,
    /** Результат выполнения (если завершён) */
    val result: String? = null,
    /** Ошибка выполнения (если произошла) */
    val error: String? = null,
    /** Временная метка */
    val timestamp: Long = System.currentTimeMillis()
)
