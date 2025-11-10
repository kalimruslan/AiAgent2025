package ru.llm.agent.repository

import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.context.ContextEntity
import ru.llm.agent.model.LlmProvider

/**
 * Реализация репозитория для управления конфигурацией LLM провайдеров.
 * Использует ContextDao для сохранения выбранного провайдера в БД.
 *
 * @param contextDao DAO для работы с контекстом диалога
 */
public class ProviderConfigRepositoryImpl(
    private val contextDao: ContextDao
) : ProviderConfigRepository {

    /**
     * Получить выбранный провайдер для диалога
     */
    override suspend fun getSelectedProvider(conversationId: String): LlmProvider {
        val context = contextDao.getContextByConversationId(conversationId)
        val providerName = context?.llmProvider

        return if (providerName != null) {
            try {
                LlmProvider.valueOf(providerName)
            } catch (e: IllegalArgumentException) {
                LlmProvider.default()
            }
        } else {
            LlmProvider.default()
        }
    }

    /**
     * Сохранить выбранный провайдер для диалога
     */
    override suspend fun saveSelectedProvider(conversationId: String, provider: LlmProvider) {
        val existingContext = contextDao.getContextByConversationId(conversationId)

        if (existingContext != null) {
            // Обновляем существующий контекст
            contextDao.upsertContext(
                existingContext.copy(llmProvider = provider.name)
            )
        } else {
            // Создаем новый контекст с провайдером
            contextDao.upsertContext(
                ContextEntity(
                    conversationId = conversationId,
                    temperature = 0.7,
                    systemprompt = "",
                    maxTokens = 500,
                    timestamp = System.currentTimeMillis(),
                    llmProvider = provider.name
                )
            )
        }
    }

    /**
     * Получить список всех доступных провайдеров
     */
    override fun getAvailableProviders(): List<LlmProvider> {
        return LlmProvider.entries
    }
}