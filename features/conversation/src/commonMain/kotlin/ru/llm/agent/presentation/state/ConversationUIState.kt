package ru.llm.agent.presentation.state

import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
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
        /** Список выбранных экспертов для режима Committee */
        val selectedExperts: List<Expert> = Expert.getPredefinedExperts().take(3), // По умолчанию первые 3
        /** Список всех доступных экспертов */
        val availableExperts: List<Expert> = Expert.getPredefinedExperts(),
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
                availableExperts = Expert.getPredefinedExperts()
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
    }
}