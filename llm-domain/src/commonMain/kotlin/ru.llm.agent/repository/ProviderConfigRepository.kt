package ru.llm.agent.repository

import ru.llm.agent.model.LlmProvider

/**
 * Репозиторий для управления конфигурацией LLM провайдеров.
 * Отвечает за сохранение и получение информации о выбранном провайдере для каждого диалога.
 */
public interface ProviderConfigRepository {
    /**
     * Получить выбранный провайдер для диалога
     *
     * @param conversationId ID диалога
     * @return Выбранный LLM провайдер или провайдер по умолчанию
     */
    public suspend fun getSelectedProvider(conversationId: String): LlmProvider

    /**
     * Сохранить выбранный провайдер для диалога
     *
     * @param conversationId ID диалога
     * @param provider Провайдер для сохранения
     */
    public suspend fun saveSelectedProvider(conversationId: String, provider: LlmProvider)

    /**
     * Получить список всех доступных провайдеров
     *
     * @return Список доступных LLM провайдеров
     */
    public fun getAvailableProviders(): List<LlmProvider>
}
