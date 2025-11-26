package ru.llm.agent.presentation.state

import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.model.conversation.ConversationMessage

internal class ConversationUIState {
    data class State(
        val messages: List<ConversationMessage>,
        val isLoading: Boolean,
        val error: String,
        val isConversationComplete: Boolean = false,
        /** Выбранный провайдер LLM */
        val selectedProvider: LlmProvider = LlmProvider.default(),
        /** Выбранный режим работы (Single AI или Committee) */
        val selectedMode: ConversationMode = ConversationMode.default(),
        /** Использованные токены (сумма из всех сообщений) */
        val usedTokens: Int = 0,
        /** Максимальное количество токенов */
        val maxTokens: Int = 8000,
        /** Токены текущего запроса (null если не подсчитывается) */
        val requestTokens: Int? = null,
        /** Информация о суммаризации истории */
        val summarizationInfo: SummarizationInfo? = null,
        /** Флаг процесса суммаризации */
        val isSummarizing: Boolean = false,

        /** ID доски Trello для умных промптов (null если не настроен) */
        val trelloBoardId: String? = null,

        /** Использовать ли RAG для поиска в базе знаний */
        val isRagEnabled: Boolean = false,

        /** Количество проиндексированных документов в RAG */
        val ragIndexedCount: Int = 0,

        /** Диалог для добавления текста в knowledge base */
        val showKnowledgeBaseDialog: Boolean = false
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                isConversationComplete = false,
                selectedProvider = LlmProvider.default(),
                selectedMode = ConversationMode.default()
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data object ResetAll : Event
        data object ClearError : Event
        data object OpenSettings : Event
        /** Выбор провайдера LLM */
        data class SelectProvider(val provider: LlmProvider) : Event
        /** Выбор режима работы */
        data class SelectMode(val mode: ConversationMode) : Event
        /** Экспортировать диалог в указанном формате */
        data class ExportConversation(val format: ExportFormat) : Event
        /** Установить ID доски Trello для умных промптов */
        data class SetTrelloBoardId(val boardId: String?) : Event
        /** Переключить использование RAG */
        data class ToggleRag(val enabled: Boolean) : Event
        /** Открыть диалог добавления знаний */
        data object ShowKnowledgeBaseDialog : Event
        /** Закрыть диалог добавления знаний */
        data object HideKnowledgeBaseDialog : Event
        /** Добавить текст в базу знаний */
        data class AddToKnowledgeBase(val text: String, val sourceId: String) : Event
        /** Очистить базу знаний */
        data object ClearKnowledgeBase : Event
    }

    /**
     * Саммари доски Trello
     */
    data class BoardSummary(
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isLoading: Boolean = false,
        /** Анализ ассистента по саммари доски */
        val assistantAnalysis: String? = null,
        /** Флаг загрузки анализа */
        val isAnalysisLoading: Boolean = false
    )
}