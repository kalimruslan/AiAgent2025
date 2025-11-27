package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.llm.agent.database.rag.RagSourceDao
import ru.llm.agent.database.rag.RagSourceEntity
import ru.llm.agent.model.rag.RagSource

/**
 * Реализация репозитория для работы с источниками RAG.
 */
public class RagSourceRepositoryImpl(
    private val ragSourceDao: RagSourceDao
) : RagSourceRepository {

    override suspend fun saveSources(
        messageId: Long,
        conversationId: String,
        sources: List<RagSource>
    ) {
        val entities = sources.map { source ->
            RagSourceEntity(
                messageId = messageId,
                conversationId = conversationId,
                index = source.index,
                text = source.text,
                sourceId = source.sourceId,
                chunkIndex = source.chunkIndex,
                similarity = source.similarity,
                timestamp = source.timestamp
            )
        }
        ragSourceDao.insertAll(entities)
    }

    override fun getSourcesForConversation(conversationId: String): Flow<List<RagSource>> {
        return ragSourceDao.getSourcesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSourcesForConversationSync(conversationId: String): List<RagSource> {
        return ragSourceDao.getSourcesForConversationSync(conversationId).map { it.toDomain() }
    }

    override suspend fun getSourcesForMessage(messageId: Long): List<RagSource> {
        return ragSourceDao.getSourcesByMessageId(messageId).map { it.toDomain() }
    }

    override suspend fun deleteSourcesForConversation(conversationId: String) {
        ragSourceDao.deleteSourcesForConversation(conversationId)
    }

    override suspend fun deleteSourcesForMessage(messageId: Long) {
        ragSourceDao.deleteSourcesForMessage(messageId)
    }

    /**
     * Маппинг Entity -> Domain
     */
    private fun RagSourceEntity.toDomain(): RagSource {
        return RagSource(
            id = id,
            messageId = messageId,
            index = index,
            text = text,
            sourceId = sourceId,
            chunkIndex = chunkIndex,
            similarity = similarity,
            timestamp = timestamp
        )
    }
}
