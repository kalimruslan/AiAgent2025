package ru.llm.agent.compose.presenter

import ru.llm.agent.model.MessageModel

internal class DiffTwoModelsUIState {
    data class State(
        val messageFirst: MessageModel.ResponseMessage?,
        val messageTwo: MessageModel.ResponseMessage?,
        val isLoading: Boolean,
        val error: String,
    ) {
        companion object Companion {
            fun empty() = State(
                messageFirst = null,
                null,
                isLoading = false,
                error = "",
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
    }
}

