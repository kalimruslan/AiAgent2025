package ru.llm.agent.model

import ru.llm.agent.model.mcp.ToolCallList
import ru.llm.agent.model.mcp.ToolResultList

public sealed class MessageModel(public open val role: Role, public open val text: String) {
    public data class PromtMessage(
        override val role: Role,
        override val text: String,
    ) : MessageModel(role, text)

    public data class UserMessage(
        override val role: Role,
        val content: String,
        val inputTokens: Int = 0,
        val isCompressed: Boolean = false,
        val notCompressedTokens: Int = 0,
        val parsedFormats: Map<String, String> = emptyMap(),
    ) : MessageModel(role, content)

    public data class ResponseMessage(
        override val role: Role,
        val content: String,
        val textFormat: PromtFormat,
        val parsedContent: ParseFromJsonModel? = null,
        val timestamp: Long = 0L,
        val tokenUsed: String = "",
        val duration: String = "",
        val toolCallList: ToolCallList? = null,
        val toolResultList: ToolResultList? = null,
    ) : MessageModel(role, content)

    public data class ToolsMessage(
        override val role: Role,
        override val text: String,
        val toolResultList: ToolResultList,
    ) : MessageModel(role, text)

    public data class NoneMessage(
        override val role: Role,
        val message: String,
    ) : MessageModel(role, message)
}

public enum class PromtFormat {
    JSON, TEXT, MARKDOWN
}

public enum class Role(public val title: String) {
    USER("user"), ASSISTANT("assistant"), SYSTEM("system"), FUNCTION("function"), NONE("none")
}