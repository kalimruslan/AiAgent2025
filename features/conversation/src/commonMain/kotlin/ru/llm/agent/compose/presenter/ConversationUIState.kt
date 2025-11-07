package ru.llm.agent.compose.presenter

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
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                isConversationComplete = false,
                selectedProvider = LlmProvider.default()
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data object ResetConversation : Event
        data object ClearError : Event
        data object OpenSettings : Event
        /** Выбор провайдера LLM */
        data class SelectProvider(val provider: LlmProvider) : Event
    }
}

