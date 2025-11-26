package ru.llm.agent.repository

import ru.llm.agent.model.rag.RagDocument
import ru.llm.agent.model.rag.RagIndexResult
import ru.llm.agent.service.EmbeddingService

/**
 * Реализация RagRepository
 */
public class RagRepositoryImpl(
    private val embeddingService: EmbeddingService
) : RagRepository {

    override suspend fun indexText(text: String, sourceId: String): RagIndexResult {
        val chunksIndexed = embeddingService.indexText(text, sourceId)
        return RagIndexResult(
            chunksIndexed = chunksIndexed,
            sourceId = sourceId
        )
    }

    override suspend fun search(
        query: String,
        topK: Int,
        threshold: Double,
        useMmr: Boolean,
        mmrLambda: Double
    ): List<RagDocument> {
        val searchResults = if (useMmr) {
            embeddingService.searchWithMmr(query, topK, threshold, mmrLambda)
        } else {
            embeddingService.search(query, topK, threshold)
        }

        return searchResults.map { result ->
            RagDocument(
                text = result.document.text,
                similarity = result.similarity,
                metadata = result.document.metadata
            )
        }
    }

    override suspend fun getIndexedCount(): Int {
        return embeddingService.getIndexedDocumentsCount()
    }

    override suspend fun clearIndex() {
        embeddingService.clearIndex()
    }
}