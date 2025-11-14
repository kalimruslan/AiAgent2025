package ru.llm.agent.model

/**
 * Форматы экспорта диалога
 */
public enum class ExportFormat(public val extension: String, public val mimeType: String) {
    JSON("json", "application/json"),
    PDF("pdf", "application/pdf"),
    TXT("txt", "text/plain"),
    XLS("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}