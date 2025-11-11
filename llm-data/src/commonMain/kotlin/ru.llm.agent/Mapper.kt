package ru.llm.agent

import ru.ai.agent.data.response.proxyapi.ProxyMessageResponse
import ru.llm.agent.data.response.yaGPT.YaMessageResponse
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextEntity
import ru.llm.agent.database.expert.ExpertOpinionEntity
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.ConversationContext
import ru.llm.agent.model.ExpertOpinion
import ru.llm.agent.model.conversation.ConversationMessage

public fun YaMessageResponse.toModel(usedTokens: String? = null, outputFormat: PromtFormat = PromtFormat.TEXT): MessageModel =
    MessageModel.ResponseMessage(
        role = Role.valueOf(role.uppercase()),
        content = if (outputFormat == PromtFormat.JSON) {
            this.text
                ?.replace(Regex("^`+"), "")
                ?.replace(Regex("`+$"), "").orEmpty()
        } else this.text.orEmpty(),
        tokenUsed = usedTokens.orEmpty(),
        textFormat = outputFormat,
        parsedContent = null,
        toolCallList = this.toolCallList,
        toolResultList = this.toolResultList
    )

public fun ProxyMessageResponse.toModel(usedTokens: String?): MessageModel = MessageModel.ResponseMessage(
    role = Role.valueOf(role.uppercase()),
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
    isComplete = false,
    originalResponse = originalResponse,
    model = model,
    inputTokens = inputTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    responseTimeMs = responseTimeMs
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

public fun ExpertOpinionEntity.toModel(): ExpertOpinion {
    return ExpertOpinion(
        id = id,
        expertId = expertId,
        expertName = expertName,
        expertIcon = expertIcon,
        messageId = messageId,
        opinion = opinion,
        timestamp = timestamp,
        originalResponse = originalResponse
    )
}