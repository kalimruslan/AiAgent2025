package ru.llm.agent.exporter

import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Экспортер диалога в PDF формат
 * Использует expect/actual для платформозависимой генерации PDF
 */
public expect class PdfConversationExporter() : ConversationExporter {
    override val format: ExportFormat
    override suspend fun export(
        conversationId: String,
        messages: List<ConversationMessage>
    ): String
}