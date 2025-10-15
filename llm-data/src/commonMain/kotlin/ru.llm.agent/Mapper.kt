package ru.llm.agent

import ru.llm.agent.data.response.YaMessageResponse
import ru.llm.agent.model.MessageModel

public fun YaMessageResponse.toModel(usedTokens: Int): MessageModel = MessageModel(
    role = this.role,
    content = this.text,
    tokenUsed = usedTokens
)