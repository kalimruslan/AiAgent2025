package ru.llm.agent


public enum class RoleSender(public val type: String) {
    USER("user"),
    SYSTEM("system"),
    ASSISTANT("assistant"),
}