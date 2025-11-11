package ru.llm.agent.repository

import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.config.LlmConfig
import ru.llm.agent.model.config.LlmTaskType
import ru.llm.agent.model.config.getConfig

/**
 * Реализация LlmConfigRepository.
 *
 * Управляет конфигурациями LLM моделей. Использует in-memory хранилище
 * для пользовательских настроек (можно расширить до БД при необходимости).
 */
public class LlmConfigRepositoryImpl : LlmConfigRepository {
    // In-memory хранилище пользовательских конфигураций
    // Key: conversationId, Value: LlmConfig
    private val userConfigs = mutableMapOf<String, LlmConfig>()

    override suspend fun getDefaultConfig(provider: LlmProvider): LlmConfig {
        return when (provider) {
            LlmProvider.YANDEX_GPT -> LlmConfig.defaultYandexGpt()
            LlmProvider.PROXY_API_GPT4O_MINI -> LlmConfig.defaultProxyApiGpt4o()
            LlmProvider.PROXY_API_MISTRAY_AI -> LlmConfig.defaultProxyApiMistral()
        }
    }

    override suspend fun getConfigForTask(
        taskType: LlmTaskType,
        provider: LlmProvider
    ): LlmConfig {
        return taskType.getConfig(provider)
    }

    override suspend fun getUserConfig(
        conversationId: String,
        provider: LlmProvider
    ): LlmConfig? {
        return userConfigs[conversationId]?.takeIf { it.provider == provider }
    }

    override suspend fun saveUserConfig(conversationId: String, config: LlmConfig) {
        userConfigs[conversationId] = config
    }

    override suspend fun removeUserConfig(conversationId: String) {
        userConfigs.remove(conversationId)
    }

    override suspend fun getAllPresetConfigs(provider: LlmProvider): Map<LlmTaskType, LlmConfig> {
        return LlmTaskType.entries.associateWith { taskType ->
            taskType.getConfig(provider)
        }
    }
}