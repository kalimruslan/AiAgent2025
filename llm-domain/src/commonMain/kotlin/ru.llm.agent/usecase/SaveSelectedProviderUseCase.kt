package ru.llm.agent.usecase

import ru.llm.agent.model.LlmProvider
import ru.llm.agent.repository.ProviderConfigRepository

/**
 * Use case для сохранения выбранного LLM провайдера для диалога
 */
public class SaveSelectedProviderUseCase(
    private val providerConfigRepository: ProviderConfigRepository
) {
    /**
     * Сохранить выбранный провайдер для диалога
     *
     * @param conversationId ID диалога
     * @param provider Провайдер для сохранения
     */
    public suspend operator fun invoke(conversationId: String, provider: LlmProvider) {
        providerConfigRepository.saveSelectedProvider(conversationId, provider)
    }
}
