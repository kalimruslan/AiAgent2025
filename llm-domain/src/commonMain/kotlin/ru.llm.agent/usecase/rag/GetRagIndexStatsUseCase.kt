package ru.llm.agent.usecase.rag

import ru.llm.agent.repository.RagRepository

/**
 * Use case для получения статистики RAG индекса
 */
public class GetRagIndexStatsUseCase(
    private val ragRepository: RagRepository
) {
    public suspend operator fun invoke(): Int {
        return ragRepository.getIndexedCount()
    }
}