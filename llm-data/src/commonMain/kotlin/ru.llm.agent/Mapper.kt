package ru.llm.agent

import ru.llm.agent.data.response.proxyapi.ProxyMessageResponse
import ru.llm.agent.data.response.yandexGPT.YaMessageResponse
import ru.llm.agent.model.MessageModel

public fun ProxyMessageResponse.toModel(usedTokens: Int): MessageModel = MessageModel(
    role = this.role,
    content = this.content,
    tokenUsed = usedTokens
)

public fun YaMessageResponse.toModel(usedTokens: Int): MessageModel = MessageModel(
    role = this.role,
    content = this.text,
    tokenUsed = usedTokens
)