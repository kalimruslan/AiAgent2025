package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.model.ConversationId
import ru.llm.agent.core.utils.model.MessageId
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.database.mappers.MessageMapper
import ru.llm.agent.model.conversation.Message
import ru.llm.agent.repository.MessageRepository

/**
 * Реализация MessageRepository с использованием Room Database.
 * Использует разделённые read/write DAO и mapper для преобразования между слоями.
 */
public class MessageRepositoryImpl(
    private val database: MessageDatabase
) : MessageRepository {

    private val readDao = database.messageReadDao()
    private val writeDao = database.messageWriteDao()

    override suspend fun getMessage(id: MessageId): Message? {
        val entity = readDao.getMessageById(id.value) ?: return null
        return MessageMapper.toDomain(entity)
    }

    override suspend fun getMessages(conversationId: ConversationId): Flow<List<Message>> {
        return readDao.getMessagesByConversation(conversationId.value)
            .map { entities -> MessageMapper.toDomainList(entities) }
    }

    override suspend fun getMessagesSync(conversationId: ConversationId): List<Message> {
        val entities = readDao.getMessagesByConversationSync(conversationId.value)
        return MessageMapper.toDomainList(entities)
    }

    override suspend fun saveUserMessage(message: Message.User): MessageId {
        val entity = MessageMapper.toEntity(message)
        val id = writeDao.insertMessage(entity)
        return MessageId(id)
    }

    override suspend fun saveAssistantMessage(message: Message.Assistant): MessageId {
        val entity = MessageMapper.toEntity(message)
        val id = writeDao.insertMessage(entity)
        return MessageId(id)
    }

    override suspend fun saveSystemMessage(message: Message.System): MessageId {
        val entity = MessageMapper.toEntity(message)
        val id = writeDao.insertMessage(entity)
        return MessageId(id)
    }

    override suspend fun updateMessage(message: Message) {
        val entity = MessageMapper.toEntity(message)
        writeDao.updateMessage(entity)
    }

    override suspend fun deleteMessage(id: MessageId) {
        writeDao.deleteMessagesByIds(listOf(id.value))
    }

    override suspend fun deleteMessages(ids: List<MessageId>) {
        val idValues = ids.map { it.value }
        writeDao.deleteMessagesByIds(idValues)
    }

    override suspend fun deleteAllMessages(conversationId: ConversationId) {
        writeDao.deleteConversation(conversationId.value)
    }

    override suspend fun getMessageCount(conversationId: ConversationId): Int {
        val messages = readDao.getMessagesByConversationSync(conversationId.value)
        return messages.size
    }

    override suspend fun getLastMessages(conversationId: ConversationId, count: Int): List<Message> {
        val entities = readDao.getMessagesByConversationSync(conversationId.value)
        val lastEntities = entities.takeLast(count)
        return MessageMapper.toDomainList(lastEntities)
    }
}