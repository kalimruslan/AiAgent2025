package ru.llm.agent.compose.presenter

import ru.llm.agent.compose.presenter.model.MessageTypeUI

internal class ChatLlmContract {
    data class State(
        val historicalMessages: List<MessageTypeUI>,
        val messages: List<MessageTypeUI>,
        val isLoading: Boolean,
        val error: String,
        val selectedAIType: AiType,
        val workMode: WorkMode
    ) {
        companion object {
            fun empty() = State(
                historicalMessages = emptyList(),
                messages = emptyList(),
                isLoading = false,
                error = "",
                selectedAIType = AiType.YaGptAI(),
                workMode = WorkMode.SINGLE
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data class SelectAIType(val aiType: AiType) : Event
    }
}

enum class WorkMode {
    SINGLE, MULTI
}
