package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.llm.agent.database.expert.ExpertOpinionDao
import ru.llm.agent.database.expert.ExpertOpinionEntity
import ru.llm.agent.model.ExpertOpinion

/**
 * Реализация репозитория для работы с мнениями экспертов
 */
public class ExpertRepositoryImpl(
    private val expertOpinionDao: ExpertOpinionDao
) : ExpertRepository {

    override suspend fun saveExpertOpinion(
        expertId: String,
        expertName: String,
        expertIcon: String,
        messageId: Long,
        conversationId: String,
        opinion: String,
        timestamp: Long,
        originalResponse: String?
    ): Long {
        val entity = ExpertOpinionEntity(
            expertId = expertId,
            expertName = expertName,
            expertIcon = expertIcon,
            messageId = messageId,
            conversationId = conversationId,
            opinion = opinion,
            timestamp = timestamp,
            originalResponse = originalResponse
        )
        return expertOpinionDao.insertOpinion(entity)
    }

    override fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinion>> {
        return expertOpinionDao.getOpinionsForMessage(messageId).map { entities ->
            entities.map { it.toExpertOpinion() }
        }
    }

    override fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinion>> {
        return expertOpinionDao.getOpinionsForConversation(conversationId).map { entities ->
            entities.map { it.toExpertOpinion() }
        }
    }

    override suspend fun deleteOpinionsForConversation(conversationId: String) {
        expertOpinionDao.deleteOpinionsForConversation(conversationId)
    }

    override suspend fun deleteOpinionsForMessage(messageId: Long) {
        expertOpinionDao.deleteOpinionsForMessage(messageId)
    }

    override suspend fun getOpinionsCountForMessage(messageId: Long): Int {
        return expertOpinionDao.getOpinionsCountForMessage(messageId)
    }

    /**
     * Преобразование Entity в доменную модель
     */
    private fun ExpertOpinionEntity.toExpertOpinion(): ExpertOpinion {
        return ExpertOpinion(
            id = id,
            expertId = expertId,
            expertName = expertName,
            expertIcon = expertIcon,
            messageId = messageId,
            opinion = opinion,
            timestamp = timestamp,
            originalResponse = originalResponse
        )
    }
}
