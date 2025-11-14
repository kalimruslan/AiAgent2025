package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.SummarizationInfo
import ru.llm.agent.repository.SummarizationRepository

/**
 * Use Case для получения информации о суммаризации истории диалога.
 * Следует принципу Clean Architecture - ViewModel зависит только от Use Case, не от Repository.
 */
public class GetSummarizationInfoUseCase(
    private val summarizationRepository: SummarizationRepository
) {
    /**
     * Получить информацию о суммаризации для диалога
     *
     * @param conversationId ID диалога
     * @return Flow с информацией о суммаризации
     */
    public suspend operator fun invoke(conversationId: String): Flow<SummarizationInfo> {
        // Преобразуем String в ConversationId
        val id = ru.llm.agent.core.utils.model.ConversationId(conversationId)
        return summarizationRepository.getSummarizationInfo(id)
    }
}