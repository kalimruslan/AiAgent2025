package ru.llm.agent

import ru.llm.agent.data.response.YaMessageResponse
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat

public fun YaMessageResponse.toModel(usedTokens: Int, outputFormat: PromtFormat): MessageModel =
    MessageModel.ResponseMessage(
        role = this.role,
        content = if (outputFormat == PromtFormat.JSON) {
            this.text
                .replace(Regex("^`+"), "")
                .replace(Regex("`+$"), "")
        } else this.text,
        tokenUsed = usedTokens,
        textFormat = outputFormat,
        parsedContent = null
    )