package ru.llm.agent.model

public sealed interface MessageModel {
    public data class PromtMessage(
        val role: Role,
        val text: String
    ) : MessageModel

    public data class UserMessage(
        val role: Role,
        val content: String,
        val parsedFormats: Map<String, String> = emptyMap(),
    ) : MessageModel

    public data class ResponseMessage(
        val role: String,
        val content: String,
        val textFormat: PromtFormat,
        val parsedContent: ParseFromJsonModel?,
        val timestamp: Long = 0L,
        val tokenUsed: Int = 0,
    ) : MessageModel
}

public enum class PromtFormat {
    JSON, TEXT, MARKDOWN
}

public enum class Role(public val title: String) {
    USER("user"), ASSISTANT("assistant"), SYSTEM("system")
}