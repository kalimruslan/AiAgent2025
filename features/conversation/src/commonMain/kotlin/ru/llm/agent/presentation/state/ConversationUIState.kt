package ru.llm.agent.presentation.state

import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.model.mcp.McpToolInfo

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
        /** Список выбранных экспертов для режима Committee */
        val selectedExperts: List<Expert> = Expert.getPredefinedExperts().take(3), // По умолчанию первые 3
        /** Список всех доступных экспертов */
        val availableExperts: List<Expert> = Expert.getPredefinedExperts(),
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
        val isUsedMcpTools: Boolean = false,

        val availableTools: List<McpToolInfo>,

        /** Текущий выполняемый MCP tool (null если не выполняется) */
        val currentToolExecution: ToolExecutionStatus? = null,

        /** Последнее саммари доски Trello */
        val boardSummary: BoardSummary? = null,

        /** ID доски Trello для умных промптов (null если не настроен) */
        val trelloBoardId: String? = null
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                isConversationComplete = false,
                selectedProvider = LlmProvider.default(),
                selectedMode = ConversationMode.default(),
                selectedExperts = Expert.getPredefinedExperts().take(3),
                availableExperts = Expert.getPredefinedExperts(),
                availableTools = emptyList(),
                currentToolExecution = null
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
        /** Переключить эксперта (добавить/убрать из выбранных) */
        data class ToggleExpert(val expert: Expert) : Event
        /** Экспортировать диалог в указанном формате */
        data class ExportConversation(val format: ExportFormat) : Event
        /** Используем ли инструменты MCP */
        data class SwitchNeedMcpTools(val useTools: Boolean) : Event
        /** Установить ID доски Trello для умных промптов */
        data class SetTrelloBoardId(val boardId: String?) : Event
    }

    /**
     * Статус выполнения MCP tool
     */
    data class ToolExecutionStatus(
        val toolName: String,
        val description: String,
        val isExecuting: Boolean = true
    )

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