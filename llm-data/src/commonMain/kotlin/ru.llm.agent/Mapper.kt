package ru.llm.agent

import ru.ai.agent.data.response.proxyapi.ProxyMessageResponse
import ru.llm.agent.data.response.yaGPT.YaMessageResponse
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextEntity
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.ConversationContext
import ru.llm.agent.model.conversation.ConversationMessage

public fun YaMessageResponse.toModel(usedTokens: String?, outputFormat: PromtFormat = PromtFormat.TEXT): MessageModel =
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

public fun ProxyMessageResponse.toModel(usedTokens: String?): MessageModel = MessageModel.ResponseMessage(
    role = this.role,
    content = this.content,
    tokenUsed = usedTokens.orEmpty(),
    textFormat = PromtFormat.TEXT,
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

public fun ConversationContext.toEntity(conversationId: String): ContextEntity = ContextEntity(
    temperature = temperature,
    systemprompt = systemPrompt,
    maxTokens = maxTokens,
    timestamp = timestamp,
    conversationId = conversationId
)

public fun ContextEntity.toModel(): ConversationContext = ConversationContext(
    temperature = temperature,
    systemPrompt = systemprompt,
    maxTokens = maxTokens,
    timestamp = timestamp
)