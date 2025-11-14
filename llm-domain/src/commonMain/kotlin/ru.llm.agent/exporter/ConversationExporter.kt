package ru.llm.agent.exporter

import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Интерфейс для экспорта диалога в различные форматы
 */
public interface ConversationExporter {
    /**
     * Формат, который поддерживает данный экспортер
     */
    public val format: ExportFormat

    /**
     * Экспортировать диалог в строковое представление
     *
     * @param conversationId ID диалога
     * @param messages Список сообщений диалога
     * @return Строковое представление диалога в указанном формате
     */
    public suspend fun export(
        conversationId: String,
        messages: List<ConversationMessage>
    ): String
}