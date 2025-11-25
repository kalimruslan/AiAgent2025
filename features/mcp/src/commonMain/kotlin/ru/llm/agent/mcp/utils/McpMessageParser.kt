package ru.llm.agent.mcp.utils

/**
 * Утилиты для парсинга сообщений, связанных с выполнением MCP инструментов
 */

/**
 * Извлечь название инструмента из текста сообщения
 * Формат: "Выполнение инструмента: tool_name\nРезультат: ..."
 */
fun String.extractToolName(): String {
    return try {
        // Пытаемся найти паттерн "Выполнение инструмента: название"
        val pattern = "Выполнение инструмента: ([^\\n]+)".toRegex()
        val match = pattern.find(this)
        match?.groupValues?.getOrNull(1)?.trim() ?: "Инструмент"
    } catch (e: Exception) {
        "Инструмент"
    }
}

/**
 * Извлечь результат выполнения из текста сообщения
 * Формат: "Результат: result_text"
 */
fun String.extractToolResult(): String {
    return try {
        // Пытаемся найти паттерн "Результат: текст"
        val pattern = "Результат: ([^\\n]+)".toRegex()
        val match = pattern.find(this)
        match?.groupValues?.getOrNull(1)?.trim() ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * Проверить, является ли сообщение результатом выполнения инструмента
 */
fun String.isToolExecutionMessage(): Boolean {
    return contains("Выполнение инструмента:")
}