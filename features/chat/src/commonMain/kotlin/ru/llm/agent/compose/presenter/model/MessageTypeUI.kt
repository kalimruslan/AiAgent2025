package ru.llm.agent.compose.presenter.model

import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat

sealed class MessageTypeUI(open val message: MessageModel?) {
    data class TheirMessageUI(override val message: MessageModel.ResponseMessage) : MessageTypeUI(message){}
    data class MyMessageUI(override val message: MessageModel.UserMessage) : MessageTypeUI(message)

    data class DateSeparatorUI(val date: String) : MessageTypeUI(message = null)
}