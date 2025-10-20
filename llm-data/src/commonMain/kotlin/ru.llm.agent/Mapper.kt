package ru.llm.agent

import ru.llm.agent.data.response.YaMessageResponse
import ru.llm.agent.database.MessageEntity
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage

public fun YaMessageResponse.toModel(usedTokens: String?, outputFormat: PromtFormat): MessageModel =
    MessageModel.ResponseMessage(
        role = this.role,
        content = if (outputFormat == PromtFormat.JSON) {
            this.text
                .replace(Regex("^`+"), "")
                .replace(Regex("`+$"), "")
        } else this.text,
        tokenUsed = usedTokens.orEmpty(),
        textFormat = outputFormat,
        parsedContent = null
    )

public fun MessageEntity.toModel(): ConversationMessage = ConversationMessage(
    id = id,
    conversationId = conversationId,
    role = Role.valueOf(role.uppercase()),
    text = text
        .replace(Regex("^`+"), "")
        .replace(Regex("`+$"), ""),
    timestamp = timestamp,
    isContinue = false,
    isComplete = false
)