package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.expert.ExpertOpinionDao
import ru.llm.agent.database.messages.MessageDao
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.toModel
import ru.llm.agent.usecase.SystemPromptBuilder

/**
 * Реализация репозитория для работы с сообщениями диалогов.
 * Отвечает за CRUD операции с сообщениями в БД и базовую инициализацию диалогов.
 *
 * @param messageDao DAO для работы с сообщениями
 * @param contextDao DAO для работы с контекстом диалога
 * @param expertOpinionDao DAO для работы с мнениями экспертов
 * @param providerConfigRepository Репозиторий для управления конфигурацией провайдеров
 * @param systemPromptBuilder Builder для создания системных промптов
 * @param logger Кроссплатформенный логгер
 */
public class ConversationRepositoryImpl(
    private val messageDao: MessageDao,
    private val contextDao: ContextDao,
    private val expertOpinionDao: ExpertOpinionDao,
    private val providerConfigRepository: ProviderConfigRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val logger: Logger,
) : ConversationRepository {

    /**
     * Инициализировать диалог с системным сообщением
     */
    override suspend fun initializeConversation(conversationId: String) {
        val existing = messageDao.getMessagesByConversationSync(conversationId)
        val context = contextDao.getContextByConversationId(conversationId)

        if (existing.isEmpty() || context?.systemprompt?.isNotEmpty() == true) {
            val provider = providerConfigRepository.getSelectedProvider(conversationId)
            val systemMessage = MessageEntity(
                conversationId = conversationId,
                role = "system",
                text = context?.systemprompt ?: systemPromptBuilder.buildDefaultAndroidConsultantPrompt(),
                timestamp = System.currentTimeMillis(),
                model = provider.displayName
            )
            messageDao.upsertSystemMessage(systemMessage)
        }
    }

    /**
     * Получить все сообщения диалога
     */
    override suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>> {
        return messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Получить сообщения вместе с мнениями экспертов (для режима Committee)
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
                    val opinions = opinionsByMessageId[message.id]?.map { it.toModel() } ?: emptyList()
                    logger.info("Message ${message.id} has ${opinions.size} opinions")
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
     * Сохранить сообщение пользователя в БД
     */
    override suspend fun saveUserMessage(
        conversationId: String,
        message: String,
        provider: LlmProvider
    ): Long {
        // Не сохраняем пустые сообщения
        if (message.isBlank()) {
            logger.info("Пропускаем сохранение пустого пользовательского сообщения")
            return -1L
        }

        // Сохраняем выбранный провайдер
        providerConfigRepository.saveSelectedProvider(conversationId, provider)

        // Сохраняем сообщение пользователя
        val userEntity = MessageEntity(
            conversationId = conversationId,
            role = "user",
            text = message,
            timestamp = System.currentTimeMillis(),
            model = provider.displayName
        )
        return messageDao.insertMessage(userEntity)
    }

    /**
     * Сохранить сообщение ассистента в БД
     */
    override suspend fun saveAssistantMessage(
        conversationMessage: ConversationMessage
    ): Long {
        // Не сохраняем пустые сообщения
        if (conversationMessage.text.isBlank()) {
            logger.info("Пропускаем сохранение пустого сообщения ассистента")
            return -1L
        }

        val assistantEntity = MessageEntity(
            conversationId = conversationMessage.conversationId,
            role = "assistant",
            text = conversationMessage.text,
            timestamp = conversationMessage.timestamp,
            originalResponse = conversationMessage.originalResponse,
            model = conversationMessage.model,
            inputTokens = conversationMessage.inputTokens,
            completionTokens = conversationMessage.completionTokens,
            totalTokens = conversationMessage.totalTokens,
            responseTimeMs = conversationMessage.responseTimeMs
        )
        return messageDao.insertMessage(assistantEntity)
    }

    /**
     * Получить все сообщения диалога синхронно (для use cases)
     */
    override suspend fun getMessagesByConversationSync(conversationId: String): List<ConversationMessage> {
        return messageDao.getMessagesByConversationSync(conversationId).map { it.toModel() }
    }

    /**
     * Очистить диалог
     */
    override suspend fun clearConversation(conversationId: String, initNew: Boolean) {
        messageDao.clearConversation(conversationId)
    }

    /**
     * Удалить диалог
     */
    override suspend fun deleteConversation(conversationId: String, initNew: Boolean) {
        messageDao.deleteAll()
        expertOpinionDao.deleteOpinionsForConversation(conversationId)
        contextDao.deleteAllContexts()
        if (initNew) initializeConversation(conversationId)
    }

    /**
     * Получить список всех диалогов
     */
    override fun getAllConversations(): Flow<List<String>> {
        return messageDao.getAllConversations()
    }

    /**
     * Получить контекст диалога (температура, system prompt, maxTokens)
     */
    override suspend fun getContext(conversationId: String): Flow<ru.llm.agent.model.ConversationContext?> {
        return contextDao.getContextByConversationIdFlow(conversationId).map { contextEntity ->
            contextEntity?.toModel()
        }
    }

    /**
     * Получить информацию о суммаризации истории диалога
     */
    override suspend fun getSummarizationInfo(conversationId: String): Flow<ru.llm.agent.model.SummarizationInfo> {
        return messageDao.getMessagesByConversation(conversationId).map { messages ->
            val summarizedMessages = messages.filter { it.isSummarized }

            // Подсчитываем сэкономленные токены
            // Это приблизительная оценка на основе количества суммаризированных сообщений
            val savedTokens = summarizedMessages.sumOf { it.totalTokens ?: 0 }

            // Находим последнюю суммаризацию
            val lastSummarizationTimestamp = summarizedMessages.maxOfOrNull { it.timestamp }

            ru.llm.agent.model.SummarizationInfo(
                hasSummarizedMessages = summarizedMessages.isNotEmpty(),
                summarizedMessagesCount = summarizedMessages.size,
                savedTokens = savedTokens,
                lastSummarizationTimestamp = lastSummarizationTimestamp
            )
        }
    }

    /**
     * Удалить сообщения по их ID
     */
    override suspend fun deleteMessages(messageIds: List<Long>) {
        if (messageIds.isNotEmpty()) {
            messageDao.deleteMessagesByIds(messageIds)
        }
    }

    /**
     * Сохранить системное сообщение (например, суммаризацию)
     */
    override suspend fun saveSystemMessage(
        conversationId: String,
        text: String,
        isSummarized: Boolean,
        totalTokens: Int?
    ): Long {
        // Не сохраняем пустые сообщения
        if (text.isBlank()) {
            logger.info("Пропускаем сохранение пустого системного сообщения")
            return -1L
        }

        val message = MessageEntity(
            conversationId = conversationId,
            role = "system",
            text = text,
            timestamp = System.currentTimeMillis(),
            model = "System",
            isSummarized = isSummarized,
            totalTokens = totalTokens
        )
        return messageDao.insertMessage(message)
    }
}