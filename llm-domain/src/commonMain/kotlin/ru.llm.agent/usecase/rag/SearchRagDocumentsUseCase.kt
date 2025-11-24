package ru.llm.agent.usecase.rag

import ru.llm.agent.model.rag.RagDocument
import ru.llm.agent.repository.RagRepository

/**
 * Use case для поиска релевантных документов в RAG системе
 */
public class SearchRagDocumentsUseCase(
    private val ragRepository: RagRepository
) {
    public suspend operator fun invoke(
        query: String,
        topK: Int = 5,
        threshold: Double = 0.3
    ): List<RagDocument> {
        return ragRepository.search(
            query = query,
            topK = topK,
            threshold = threshold
        )
    }
}