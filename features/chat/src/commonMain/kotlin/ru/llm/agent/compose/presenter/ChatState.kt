package ru.llm.agent.compose.presenter

import ru.llm.agent.model.MessageModel

internal class ChatLlmContract {
    data class State(
        val messages: List<MessageModel>,
        val isLoading: Boolean,
        val error: String?,
        val userMessage: String,
        val selectedAIType: AiType,
        val workMode: WorkMode
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                userMessage = "",
                selectedAIType = AiType.ProxyAI(),
                workMode = WorkMode.SINGLE
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data class SelectAIType(val aiType: AiType) : Event
        data class SelectProxyAiModel(val model: AiType.ProxyAI.ProxyAIModel) : Event
    }
}

enum class WorkMode {
    SINGLE, MULTI
}
