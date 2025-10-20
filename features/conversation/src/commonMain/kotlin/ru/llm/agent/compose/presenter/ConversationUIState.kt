package ru.llm.agent.compose.presenter

import ru.llm.agent.model.conversation.ConversationMessage

internal class ConversationUIState {
    data class State(
        val messages: List<ConversationMessage>,
        val isLoading: Boolean,
        val error: String,
        val isConversationComplete: Boolean = false,
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                isConversationComplete = false
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data object ResetConversation : Event
        data object ClearError : Event

        data object OpenSettings : Event
    }
}

