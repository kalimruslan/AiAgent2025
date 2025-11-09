package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.ai.agent.data.request.proxyapi.ProxyApiRequest
import ru.ai.agent.data.request.proxyapi.ProxyMessageRequest
import ru.ai.agent.data.response.proxyapi.ProxyApiResponse
import ru.llm.agent.NetworkResult
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.api.YandexApi
import ru.llm.agent.data.request.yaGPT.CompletionOptions
import ru.llm.agent.data.request.yaGPT.YaMessageRequest
import ru.llm.agent.data.request.yaGPT.YaRequest
import ru.llm.agent.data.response.yaGPT.YandexGPTResponse
import ru.llm.agent.database.messages.MessageDao
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.expert.ExpertOpinionDao
import ru.llm.agent.database.expert.ExpertOpinionEntity
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.AssistantJsonAnswer
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi
import java.util.logging.Logger

/**
 * Репа для работы с диалогами
 * @param messageDao Dao для работы с сообщениями
 * @param yandexApi Api для работы с Yandex API
 * @param proxyApi Api для работы с ProxyAPI
 * @param contextDao Dao для работы с контекстом
 * @param expertRepository Репозиторий для работы с мнениями экспертов
 * @param expertOpinionDao Dao для работы с мнениями экспертов
 */
public class ConversationRepositoryImpl(
    private val messageDao: MessageDao,
    private val yandexApi: YandexApi,
    private val proxyApi: ProxyApi,
    private val contextDao: ContextDao,
    private val expertRepository: ExpertRepository,
    private val expertOpinionDao: ExpertOpinionDao,
) : ConversationRepository {
    /**
     * Инициализируем диалог, смотрим пустой ли он, если да, то добавляем системное сообщение
     */
    override suspend fun initializeConversation(conversationId: String) {
        val existing = messageDao.getMessagesByConversationSync(conversationId)
        val context = contextDao.getContextByConversationId(conversationId)

        if (existing.isEmpty() || context?.systemprompt?.isNotEmpty() == true) {
            val systemMessage = MessageEntity(
                conversationId = conversationId,
                role = "system",
                text = context?.systemprompt
                    ?: """
                    Ты — консультант по Андроид разработке.

                    ПРАВИЛА ДИАЛОГА:
                    1. Задавай уточняющие вопросы, чтобы понять идею пользователя
                    2. Когда соберешь достаточно информации (не более 3 вопроса), дай финальный совет

                    Отвечай строго в JSON формате по следующей схеме:
                    {
                      "answer": "текст ответа",
                      "is_continue": "флаг, если нужно продолжить диалог, например true или false",
                      "is_complete": "флаг, если готов дать финальный ответ, например true или false",
                    }
                    Не добавляй никакого текста до или после JSON.
                """.trimIndent(),
                timestamp = System.currentTimeMillis(),
                model = getSelectedProvider(conversationId).displayName
            )
            messageDao.upsertSystemMessage(systemMessage)
        }
    }

    /**
     * Получаем все сообщения по диалогу
     */
    override suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>> {
        return messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Получаем сообщения вместе с мнениями экспертов (для режима Committee)
     */
    override suspend fun getMessagesWithExpertOpinions(conversationId: String): Flow<List<ConversationMessage>> {
        // Комбинируем два Flow: сообщения и мнения экспертов
        return messageDao.getMessagesByConversation(conversationId).combine(
            expertOpinionDao.getOpinionsForConversation(conversationId)
        ) { messageEntities, allOpinions ->

            // Группируем мнения по messageId
            val opinionsByMessageId = allOpinions.groupBy { it.messageId }

            // Преобразуем entities в модели
            val messages = messageEntities.map { it.toModel() }

            // Для каждого сообщения пользователя добавляем мнения экспертов
            val result = messages.map { message ->
                if (message.role == Role.USER) {
                    val opinions = opinionsByMessageId[message.id]?.map { it.toExpertOpinion() } ?: emptyList()
                    Logger.getLogger("Committe").info("Message ${message.id} has ${opinions.size} opinions")
                    message.copy(expertOpinions = opinions)
                } else {
                    message
                }
            }

            result
        }.distinctUntilChanged { old, new ->
            // Сравниваем списки: количество сообщений и количество мнений в каждом
            val oldSignature = old.map { "${it.id}:${it.expertOpinions.size}" }.joinToString(",")
            val newSignature = new.map { "${it.id}:${it.expertOpinions.size}" }.joinToString(",")
            val isEqual = oldSignature == newSignature

            isEqual
        }
    }

    /**
     * Преобразование Entity в доменную модель ExpertOpinion
     */
    private fun ExpertOpinionEntity.toExpertOpinion(): ru.llm.agent.model.ExpertOpinion {
        return ru.llm.agent.model.ExpertOpinion(
            id = id,
            expertId = expertId,
            expertName = expertName,
            expertIcon = expertIcon,
            messageId = messageId,
            opinion = opinion,
            timestamp = timestamp,
            originalResponse = originalResponse
        )
    }

    /**
     * Отправляем сообщение в диалог
     */
    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider,
    ): Flow<NetworkResult<ConversationMessage>> {
        val context = contextDao.getContextByConversationId(conversationId)

        // Сохраняем выбранный провайдер
        saveSelectedProvider(conversationId, provider)

        // Сохраняем сообщение пользователя
        val userEntity = MessageEntity(
            conversationId = conversationId,
            role = "user",
            text = message,
            timestamp = System.currentTimeMillis(),
            model = getSelectedProvider(conversationId).displayName
        )
        messageDao.insertMessage(userEntity)

        // Получаем всю историю
        val allMessages = messageDao.getMessagesByConversationSync(conversationId)
            .map { it.toModel() }

        // Выбираем API в зависимости от провайдера
        return when (provider) {
            LlmProvider.YANDEX_GPT -> sendMessageToYandex(
                conversationId = conversationId,
                allMessages = allMessages,
                context = context,
                provider = provider
            )
            LlmProvider.PROXY_API_GPT4O_MINI, LlmProvider.PROXY_API_MISTRAY_AI-> sendMessageToProxy(
                conversationId = conversationId,
                allMessages = allMessages,
                context = context,
                provider = provider
            )
        }
    }

    /**
     * Отправляем сообщение с кастомным системным промптом (для экспертов)
     * Этот метод НЕ сохраняет системный промпт и пользовательское сообщение в БД,
     * а сразу отправляет запрос к LLM с правильными ролями
     */
    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider,
        systemPrompt: String,
    ): Flow<NetworkResult<ConversationMessage>> {
        val context = contextDao.getContextByConversationId(conversationId)

        // Сохраняем выбранный провайдер
        saveSelectedProvider(conversationId, provider)

        // Формируем список сообщений с правильными ролями: system + user
        val messages = listOf(
            ConversationMessage(
                id = 0L,
                conversationId = conversationId,
                role = Role.SYSTEM,
                text = systemPrompt,
                timestamp = System.currentTimeMillis(),
                model = provider.displayName
            ),
            ConversationMessage(
                id = 0L,
                conversationId = conversationId,
                role = Role.USER,
                text = message,
                timestamp = System.currentTimeMillis(),
                model = provider.displayName
            )
        )

        // Выбираем API в зависимости от провайдера
        return when (provider) {
            LlmProvider.YANDEX_GPT -> sendMessageToYandex(
                conversationId = conversationId,
                allMessages = messages,
                context = context,
                provider = provider
            )
            LlmProvider.PROXY_API_GPT4O_MINI, LlmProvider.PROXY_API_MISTRAY_AI -> sendMessageToProxy(
                conversationId = conversationId,
                allMessages = messages,
                context = context,
                provider = provider
            )
        }
    }

    /**
     * Отправка сообщения через Yandex API
     */
    private suspend fun sendMessageToYandex(
        conversationId: String,
        allMessages: List<ConversationMessage>,
        context: ru.llm.agent.database.context.ContextEntity?,
        provider: LlmProvider
    ): Flow<NetworkResult<ConversationMessage>> {
        val request = YaRequest(
            modelUri = provider.modelId,
            completionOptions = CompletionOptions(
                temperature = context?.temperature ?: 0.1,
                maxTokens = context?.maxTokens ?: 500
            ),
            messages = allMessages.map {
                YaMessageRequest(role = it.role.title, text = it.text)
            }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            val messageText =
                response.result.alternatives.firstOrNull()?.message?.text
                    ?.replace(Regex("^`+"), "")
                    ?.replace(Regex("`+$"), "")
                    ?: throw Exception("Empty response from API")

            // Парсим статус
            val parsed = Json.decodeFromString<AssistantJsonAnswer>(messageText)

            // Сохраняем ответ ассистента
            val assistantEntity = MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                text = parsed.answer.orEmpty(),
                timestamp = System.currentTimeMillis(),
                originalResponse = messageText,
                model = getSelectedProvider(conversationId).displayName
            )

            val assistantId = messageDao.insertMessage(assistantEntity)

            ConversationMessage(
                id = assistantId,
                conversationId = conversationId,
                role = Role.ASSISTANT,
                text = parsed.answer.orEmpty(),
                timestamp = assistantEntity.timestamp,
                isContinue = parsed.isCOntinue == true,
                isComplete = parsed.isComplete == true,
                originalResponse = messageText,
                model = getSelectedProvider(conversationId).displayName
            )
        }
    }

    /**
     * Отправка сообщения через Proxy API
     */
    private suspend fun sendMessageToProxy(
        conversationId: String,
        allMessages: List<ConversationMessage>,
        context: ru.llm.agent.database.context.ContextEntity?,
        provider: LlmProvider
    ): Flow<NetworkResult<ConversationMessage>> {
        val request = ProxyApiRequest(
            model = provider.modelId,
            temperature = context?.temperature ?: 0.7,
            maxTokens = context?.maxTokens ?: 500,
            messages = allMessages.map {
                ProxyMessageRequest(role = it.role.title, text = it.text)
            }
        )

        val result = handleApi<ProxyApiResponse> {
            proxyApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: ProxyApiResponse ->
            val messageText = response.choices.firstOrNull()?.message?.content
                ?.replace(Regex("^`+"), "")
                ?.replace(Regex("`+$"), "")
                ?: throw Exception("Empty response from API")

            // Парсим статус
            val parsed = Json.decodeFromString<AssistantJsonAnswer>(messageText)

            // Сохраняем ответ ассистента
            val assistantEntity = MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                text = parsed.answer.orEmpty(),
                timestamp = System.currentTimeMillis(),
                originalResponse = messageText,
                model = getSelectedProvider(conversationId).displayName
            )

            val assistantId = messageDao.insertMessage(assistantEntity)

            ConversationMessage(
                id = assistantId,
                conversationId = conversationId,
                role = Role.ASSISTANT,
                text = parsed.answer.orEmpty(),
                timestamp = assistantEntity.timestamp,
                isContinue = parsed.isCOntinue == true,
                isComplete = parsed.isComplete == true,
                originalResponse = messageText,
                model = getSelectedProvider(conversationId).displayName
            )
        }
    }

    /**
     * Очищаем диалог
     */
    override suspend fun clearConversation(conversationId: String, initNew: Boolean) {
        messageDao.clearConversation(conversationId)
    }

    /**
     * Удаляем диалог
     */
    override suspend fun deleteConversation(conversationId: String, initNew: Boolean) {
        messageDao.deleteAll()
        expertOpinionDao.deleteOpinionsForConversation(conversationId)
        contextDao.deleteAllContexts()
        if(initNew) initializeConversation(conversationId)
    }

    /**
     * Получаем все диалоги
     */
    override fun getAllConversations(): Flow<List<String>> {
        return messageDao.getAllConversations()
    }

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
            // Создаем новый контекст с провайдером по умолчанию
            contextDao.upsertContext(
                ru.llm.agent.database.context.ContextEntity(
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
     * Сохранить только сообщение пользователя без отправки к LLM
     */
    override suspend fun saveUserMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider
    ): Long {
        // Сохраняем выбранный провайдер
        saveSelectedProvider(conversationId, provider)

        // Сохраняем сообщение пользователя
        val userEntity = MessageEntity(
            conversationId = conversationId,
            role = "user",
            text = message,
            timestamp = System.currentTimeMillis(),
            model = getSelectedProvider(conversationId).displayName
        )
        return messageDao.insertMessage(userEntity)
    }
}