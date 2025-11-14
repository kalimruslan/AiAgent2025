package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.core.utils.model.TokenCount
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.database.mappers.MessageMapper
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.model.conversation.Message
import ru.llm.agent.repository.SummarizationRepository
import ru.llm.agent.repository.SummarizedMessage

/**
 * Реализация SummarizationRepository с использованием Room Database.
 * Управляет суммаризацией истории диалогов.
 */
public class SummarizationRepositoryImpl(
    private val database: MessageDatabase
) : SummarizationRepository {

    private val messageReadDao = database.messageReadDao()
    private val messageWriteDao = database.messageWriteDao()

    override suspend fun getSummarizationInfo(conversationId: ConversationId): Flow<SummarizationInfo> {
        return messageReadDao.getMessagesByConversation(conversationId.value)
            .map { entities ->
                val summarizedMessages = entities.filter { it.isSummarized }
                val summarizedCount = summarizedMessages.size
                val lastSummarized = summarizedMessages.maxByOrNull { it.timestamp }

                // Подсчитываем сэкономленные токены (приблизительно)
                val savedTokens = summarizedMessages.sumOf { it.totalTokens ?: 0 }

                SummarizationInfo(
                    hasSummarizedMessages = summarizedCount > 0,
                    summarizedMessagesCount = summarizedCount,
                    savedTokens = savedTokens,
                    lastSummarizationTimestamp = lastSummarized?.timestamp
                )
            }
    }

    override suspend fun saveSummarization(
        conversationId: ConversationId,
        summarizedText: String,
        originalMessageIds: List<MessageId>,
        tokensCount: TokenCount
    ): MessageId {
        // Создаём суммаризированное сообщение (как системное)
        val summarizedMessage = MessageEntity(
            id = 0, // autoGenerate
            conversationId = conversationId.value,
            role = "system",
            text = summarizedText,
            timestamp = 0L, // Заполнится при вставке
            model = "summarization",
            inputTokens = null,
            completionTokens = null,
            totalTokens = tokensCount.value,
            responseTimeMs = null,
            isSummarized = true
        )

        // Сохраняем суммаризированное сообщение
        val id = messageWriteDao.insertMessage(summarizedMessage)

        // Удаляем оригинальные сообщения
        val idsToDelete = originalMessageIds.map { it.value }
        messageWriteDao.deleteMessagesByIds(idsToDelete)

        return MessageId(id)
    }

    override suspend fun getSummarizedMessages(conversationId: ConversationId): List<SummarizedMessage> {
        val entities = messageReadDao.getSummarizedMessages(conversationId.value)

        return entities.map { entity ->
            SummarizedMessage(
                id = MessageId(entity.id),
                conversationId = conversationId,
                text = entity.text,
                originalMessageIds = emptyList(), // Информация утеряна после удаления
                originalMessageCount = 0, // Информация утеряна
                tokens = TokenCount(entity.totalTokens ?: 0),
                timestamp = entity.timestamp
            )
        }
    }

    override suspend fun isSummarized(messageId: MessageId): Boolean {
        val entity = messageReadDao.getMessageById(messageId.value) ?: return false
        return entity.isSummarized
    }

    override suspend fun getOriginalMessages(summarizedMessageId: MessageId): List<MessageId> {
        // В текущей реализации информация об оригинальных сообщениях не сохраняется
        // после удаления. Возвращаем пустой список.
        // Для полноценной реализации нужна отдельная таблица связей.
        return emptyList()
    }

    override suspend fun deleteSummarization(summarizedMessageId: MessageId) {
        messageWriteDao.deleteMessagesByIds(listOf(summarizedMessageId.value))
    }
}