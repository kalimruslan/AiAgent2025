package ru.llm.agent.repository

import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.config.LlmConfig
import ru.llm.agent.model.config.LlmTaskType

/**
 * Repository для управления конфигурациями LLM моделей.
 *
 * Предоставляет централизованный доступ к настройкам моделей,
 * позволяет сохранять пользовательские настройки и получать
 * подходящие конфигурации для разных типов задач.
 */
public interface LlmConfigRepository {
    /**
     * Получить конфигурацию по умолчанию для указанного провайдера
     *
     * @param provider Провайдер LLM
     * @return Конфигурация по умолчанию
     */
    public suspend fun getDefaultConfig(provider: LlmProvider): LlmConfig

    /**
     * Получить конфигурацию для конкретного типа задачи
     *
     * @param taskType Тип задачи
     * @param provider Провайдер LLM
     * @return Подходящая конфигурация
     */
    public suspend fun getConfigForTask(
        taskType: LlmTaskType,
        provider: LlmProvider
    ): LlmConfig

    /**
     * Получить сохраненную пользовательскую конфигурацию для конкретной задачи
     *
     * @param conversationId ID разговора
     * @param provider Провайдер LLM
     * @return Пользовательская конфигурация или null, если не задана
     */
    public suspend fun getUserConfig(
        conversationId: String,
        provider: LlmProvider
    ): LlmConfig?

    /**
     * Сохранить пользовательскую конфигурацию для конкретной задачи
     *
     * @param conversationId ID разговора
     * @param config Конфигурация для сохранения
     */
    public suspend fun saveUserConfig(
        conversationId: String,
        config: LlmConfig
    )

    /**
     * Получить актуальную конфигурацию для использования:
     * 1. Пользовательская конфигурация, если есть
     * 2. Иначе конфигурация для типа задачи
     * 3. Иначе конфигурация по умолчанию
     *
     * @param conversationId ID разговора
     * @param taskType Тип задачи
     * @param provider Провайдер LLM
     * @return Актуальная конфигурация
     */
    public suspend fun getActiveConfig(
        conversationId: String,
        taskType: LlmTaskType,
        provider: LlmProvider
    ): LlmConfig {
        return getUserConfig(conversationId, provider)
            ?: getConfigForTask(taskType, provider)
    }

    /**
     * Удалить пользовательскую конфигурацию для конкретной задачи
     *
     * @param conversationId ID разговора
     */
    public suspend fun removeUserConfig(conversationId: String)

    /**
     * Получить список всех доступных предустановленных конфигураций
     *
     * @return Map с типом задачи и соответствующей конфигурацией
     */
    public suspend fun getAllPresetConfigs(provider: LlmProvider): Map<LlmTaskType, LlmConfig>
}