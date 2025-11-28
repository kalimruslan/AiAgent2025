package ru.llm.agent.rag.presentation.state

/**
 * События для управления RAG функциональностью
 */
sealed interface RagEvent {
    /** Переключить использование RAG */
    data class ToggleEnabled(val enabled: Boolean) : RagEvent

    /** Загрузить статистику индекса */
    data object LoadStats : RagEvent

    /** Показать диалог добавления знаний */
    data object ShowKnowledgeBaseDialog : RagEvent

    /** Скрыть диалог добавления знаний */
    data object HideKnowledgeBaseDialog : RagEvent

    /** Добавить текст в базу знаний */
    data class AddToKnowledgeBase(
        val text: String,
        val sourceId: String
    ) : RagEvent

    /** Очистить базу знаний */
    data object ClearKnowledgeBase : RagEvent

    /** Изменить порог схожести */
    data class SetThreshold(val threshold: Double) : RagEvent

    /** Изменить количество документов */
    data class SetTopK(val topK: Int) : RagEvent

    /** Переключить использование MMR */
    data class ToggleMmr(val enabled: Boolean) : RagEvent

    /** Изменить lambda параметр MMR */
    data class SetMmrLambda(val lambda: Double) : RagEvent

    /** Переключить панель настроек */
    data object ToggleSettings : RagEvent

    /** Очистить ошибку */
    data object ClearError : RagEvent

    /** Очистка истории операций */
    data object ClearHistory : RagEvent
}
