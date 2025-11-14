package ru.llm.agent.exporter

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.conversation.ConversationMessage

/**
 * Экспортер диалога в JSON формат
 */
public class JsonConversationExporter : ConversationExporter {
    override val format: ExportFormat = ExportFormat.JSON

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun export(
        conversationId: String,
        messages: List<ConversationMessage>
    ): String {
        val exportData = ConversationExportData(
            conversationId = conversationId,
            exportTimestamp = System.currentTimeMillis(),
            messagesCount = messages.size,
            messages = messages.map { it.toExportMessage() }
        )
        return json.encodeToString(exportData)
    }

    /**
     * Преобразовать ConversationMessage в ExportMessage для сериализации
     */
    private fun ConversationMessage.toExportMessage(): ExportMessage {
        return ExportMessage(
            id = id,
            role = role.name,
            text = text,
            timestamp = timestamp,
            model = model,
            originalResponse = originalResponse,
            isSummarized = isSummarized,
            expertOpinions = expertOpinions.map { opinion ->
                ExportExpertOpinion(
                    expertId = opinion.expertId,
                    expertName = opinion.expertName,
                    expertIcon = opinion.expertIcon,
                    opinion = opinion.opinion,
                    timestamp = opinion.timestamp,
                    originalResponse = opinion.originalResponse
                )
            },
            tokenUsage = if (inputTokens != null || completionTokens != null || totalTokens != null) {
                ExportTokenUsage(
                    inputTokens = inputTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens,
                    responseTimeMs = responseTimeMs
                )
            } else null
        )
    }
}

/**
 * Структура данных для экспорта диалога
 */
@Serializable
private data class ConversationExportData(
    val conversationId: String,
    val exportTimestamp: Long,
    val messagesCount: Int,
    val messages: List<ExportMessage>
)

/**
 * Сообщение для экспорта
 */
@Serializable
private data class ExportMessage(
    val id: Long,
    val role: String,
    val text: String,
    val timestamp: Long,
    val model: String,
    val originalResponse: String? = null,
    val isSummarized: Boolean = false,
    val expertOpinions: List<ExportExpertOpinion> = emptyList(),
    val tokenUsage: ExportTokenUsage? = null
)

/**
 * Мнение эксперта для экспорта
 */
@Serializable
private data class ExportExpertOpinion(
    val expertId: String,
    val expertName: String,
    val expertIcon: String,
    val opinion: String,
    val timestamp: Long,
    val originalResponse: String? = null
)

/**
 * Использование токенов для экспорта
 */
@Serializable
private data class ExportTokenUsage(
    val inputTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val responseTimeMs: Long? = null
)