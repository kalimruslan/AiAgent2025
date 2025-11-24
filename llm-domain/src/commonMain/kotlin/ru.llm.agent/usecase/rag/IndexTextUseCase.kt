package ru.llm.agent.usecase.rag

import ru.llm.agent.model.rag.RagIndexResult
import ru.llm.agent.repository.RagRepository

/**
 * Use case для индексации текста в RAG системе
 */
public class IndexTextUseCase(
    private val ragRepository: RagRepository
) {
    public suspend operator fun invoke(text: String, sourceId: String): RagIndexResult {
        return ragRepository.indexText(
            text = text,
            sourceId = sourceId
        )
    }
}