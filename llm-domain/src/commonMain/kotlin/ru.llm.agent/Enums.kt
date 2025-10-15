package ru.llm.agent

public enum class OutputFormat  {
    JSON,
    CSV,
    MARKDOWN,
    TEXT;
}

public enum class RoleSender(public val type: String) {
    USER("user"),
    SYSTEM("system"),
    ASSISTANT("assistant"),
}