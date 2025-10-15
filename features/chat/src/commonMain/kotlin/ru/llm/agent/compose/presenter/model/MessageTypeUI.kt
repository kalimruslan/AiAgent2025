package ru.llm.agent.compose.presenter.model

import ru.llm.agent.model.MessageModel

sealed class MessageTypeUI(open val message: MessageModel?) {
    data class TheirMessageUI(override val message: MessageModel) : MessageTypeUI(message)
    data class MyMessageUI(override val message: MessageModel) : MessageTypeUI(message)

    data class DateSeparatorUI(val date: String) : MessageTypeUI(message = null)
}