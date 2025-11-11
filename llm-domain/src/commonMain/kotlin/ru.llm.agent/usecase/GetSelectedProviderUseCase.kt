package ru.llm.agent.usecase

import ru.llm.agent.model.LlmProvider
import ru.llm.agent.repository.ProviderConfigRepository

/**
 * Use case для получения выбранного LLM провайдера для диалога
 */
public class GetSelectedProviderUseCase(
    private val providerConfigRepository: ProviderConfigRepository
) {
    /**
     * Получить выбранный провайдер для диалога
     *
     * @param conversationId ID диалога
     * @return Выбранный LLM провайдер или провайдер по умолчанию
     */
    public suspend operator fun invoke(conversationId: String): LlmProvider {
        return providerConfigRepository.getSelectedProvider(conversationId)
    }
}
