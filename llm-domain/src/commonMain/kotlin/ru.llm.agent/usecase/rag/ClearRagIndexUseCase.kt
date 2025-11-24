package ru.llm.agent.usecase.rag

import ru.llm.agent.repository.RagRepository

/**
 * Use case для очистки RAG индекса
 */
public class ClearRagIndexUseCase(
    private val ragRepository: RagRepository
) {
    public suspend operator fun invoke() {
        ragRepository.clearIndex()
    }
}