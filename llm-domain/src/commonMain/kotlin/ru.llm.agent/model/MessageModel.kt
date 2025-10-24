package ru.llm.agent.model

public sealed class MessageModel(public open val text: String) {
    public data class PromtMessage(
        val role: Role,
        override val text: String
    ) : MessageModel(text)

    public data class UserMessage(
        val role: Role,
        val content: String,
        val inputTokens: Int = 0,
        val isCompressed: Boolean = false,
        val notCompressedTokens: Int = 0,
        val parsedFormats: Map<String, String> = emptyMap(),
    ) : MessageModel(content)

    public data class ResponseMessage(
        val role: String,
        val content: String,
        val textFormat: PromtFormat,
        val parsedContent: ParseFromJsonModel? = null,
        val timestamp: Long = 0L,
        val tokenUsed: String = "",
        val duration: String = ""
    ) : MessageModel(content)
}

public enum class PromtFormat {
    JSON, TEXT, MARKDOWN
}

public enum class Role(public val title: String) {
    USER("user"), ASSISTANT("assistant"), SYSTEM("system")
}