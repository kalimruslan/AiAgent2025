package ru.llm.agent.compose.presenter

import ru.llm.agent.model.MessageModel

internal class InteractionTwoAgentsUIState {
    data class State(
        val agentFirstMessage: MessageModel.ResponseMessage?,
        val agentSecondMessage: MessageModel.ResponseMessage?,
        val isLoading: Boolean,
        val error: String,
    ) {
        companion object Companion {
            fun empty() = State(
                agentFirstMessage = null,
                agentSecondMessage = null,
                isLoading = false,
                error = "",
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
    }
}

