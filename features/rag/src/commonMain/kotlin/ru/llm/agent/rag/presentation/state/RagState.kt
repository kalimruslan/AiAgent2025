package ru.llm.agent.rag.presentation.state

import ru.llm.agent.rag.model.RagOperationStatus

/**
 * State для RAG feature модуля
 */
data class RagState(
    /** Включен ли RAG */
    val isEnabled: Boolean = false,
    /** Количество проиндексированных документов/чанков */
    val indexedCount: Int = 0,
    /** Загружается ли статистика */
    val isLoadingStats: Boolean = false,
    /** Идёт ли процесс индексации */
    val isIndexing: Boolean = false,
    /** Текущая операция (индексация, очистка и т.д.) */
    val currentOperation: RagOperationStatus? = null,
    /** История последних 5 операций */
    val operationHistory: List<RagOperationStatus> = emptyList(),
    /** Ошибка */
    val error: String? = null,
    /** Показывать ли диалог добавления знаний */
    val showKnowledgeBaseDialog: Boolean = false,
    /** Развёрнута ли панель настроек */
    val isSettingsExpanded: Boolean = false,

    // === Настройки RAG ===

    /** Минимальный порог схожести (0.0 - 1.0) */
    val threshold: Double = 0.3,
    /** Количество возвращаемых документов */
    val topK: Int = 3,
    /** Использовать MMR (Maximum Marginal Relevance) */
    val useMmr: Boolean = true,
    /** Lambda параметр MMR (0.0 = разнообразие, 1.0 = релевантность) */
    val mmrLambda: Double = 0.5
) {
    companion object {
        fun empty() = RagState()
    }
}
