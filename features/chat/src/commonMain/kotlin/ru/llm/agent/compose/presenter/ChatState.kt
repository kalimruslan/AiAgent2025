package ru.llm.agent.compose.presenter

import ru.llm.agent.compose.presenter.model.MessageTypeUI
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat

internal class ChatLlmContract {
    data class State(
        val messages: List<MessageTypeUI>,
        val isLoading: Boolean,
        val error: String,
        val selectedAIType: AiType,
        val outputFormat: PromtFormat
    ) {
        companion object {
            fun empty() = State(
                messages = emptyList(),
                isLoading = false,
                error = "",
                selectedAIType = AiType.YaGptAI(),
                outputFormat = PromtFormat.TEXT
            )
        }
    }

    sealed interface Event {
        data class SendMessage(val message: String) : Event
        data class SelectAIType(val aiType: AiType) : Event
        data class SelectOutputFormat(val outputFormat: PromtFormat) : Event

        data class OnParseClick(val message: MessageModel.ResponseMessage) : Event
    }
}

