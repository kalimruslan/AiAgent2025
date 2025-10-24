package ru.llm.agent.compose.presenter

import ru.llm.agent.model.MessageModel

class TokensUIState {
    data class State(
        val inputTokens: Int,
        val messages: List<MessageModel>,
        val completionTokens: Int,
        val isLoading: Boolean,
        val error: String,
    ) {
        companion object Companion {
            fun empty() = State(
                inputTokens = 0,
                messages = emptyList(),
                completionTokens = 0,
                isLoading = false,
                error = "",
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
    }
}

