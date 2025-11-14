package ru.llm.agent.presentation.state

import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Sealed interface для представления состояния экрана диалога.
 * Использует MVI pattern для типобезопасного управления состоянием.
 */
public sealed interface ConversationViewState {
    /**
     * Начальное состояние - загрузка данных
     */
    public data object Loading : ConversationViewState

    /**
     * Основное рабочее состояние с контентом
     */
    public data class Content(
        /** Список сообщений диалога */
        val messages: List<ConversationMessage>,
        /** Выбранный провайдер LLM */
        val selectedProvider: LlmProvider,
        /** Выбранный режим работы (Single AI или Committee) */
        val selectedMode: ConversationMode,
        /** Список выбранных экспертов для режима Committee */
        val selectedExperts: List<Expert>,
        /** Список всех доступных экспертов */
        val availableExperts: List<Expert>,
        /** Использованные токены */
        val usedTokens: Int,
        /** Максимальное количество токенов */
        val maxTokens: Int,
        /** Токены текущего запроса */
        val requestTokens: Int? = null,
        /** Информация о суммаризации истории */
        val summarizationInfo: SummarizationInfo? = null,
        /** Флаг отправки сообщения */
        val isSendingMessage: Boolean = false,
        /** Флаг процесса суммаризации */
        val isSummarizing: Boolean = false,
        /** Флаг завершённости диалога */
        val isConversationComplete: Boolean = false
    ) : ConversationViewState {
        /** Процент использования токенов */
        val tokenUsagePercent: Float
            get() = if (maxTokens > 0) (usedTokens.toFloat() / maxTokens) * 100f else 0f

        /** Приближается ли лимит токенов (больше 75%) */
        val isNearTokenLimit: Boolean
            get() = tokenUsagePercent >= 75f

        companion object {
            fun empty(): Content = Content(
                messages = emptyList(),
                selectedProvider = LlmProvider.default(),
                selectedMode = ConversationMode.default(),
                selectedExperts = Expert.getPredefinedExperts().take(3),
                availableExperts = Expert.getPredefinedExperts(),
                usedTokens = 0,
                maxTokens = 8000
            )
        }
    }

    /**
     * Состояние ошибки
     */
    public data class Error(
        /** Сообщение об ошибке */
        val message: String,
        /** Предыдущее состояние для возможности восстановления */
        val previousState: Content?
    ) : ConversationViewState
}