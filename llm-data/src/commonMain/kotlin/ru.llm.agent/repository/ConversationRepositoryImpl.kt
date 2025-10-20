package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.llm.agent.NetworkResult
import ru.llm.agent.api.YandexApi
import ru.llm.agent.data.request.CompletionOptions
import ru.llm.agent.data.request.YaMessageRequest
import ru.llm.agent.data.request.YaRequest
import ru.llm.agent.data.response.YandexGPTResponse
import ru.llm.agent.database.MessageDao
import ru.llm.agent.database.MessageEntity
import ru.llm.agent.database.settings.SettingsDao
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.AssistantJsonAnswer
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.model.conversation.ConversationState
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi
import java.util.logging.Logger

public class ConversationRepositoryImpl(
    private val messageDao: MessageDao,
    private val yandexApi: YandexApi,
    private val settingsDao: SettingsDao
) : ConversationRepository {
    override suspend fun initializeConversation(conversationId: String) {
        val existing = messageDao.getMessagesByConversationSync(conversationId)
        val settings = settingsDao.getLastSettings()
        if (existing.isEmpty()) {
            val systemMessage = MessageEntity(
                conversationId = conversationId,
                role = "system",
                text = settings?.systemprompt
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
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(systemMessage)
        }
    }

    override suspend fun getMessages(conversationId: String): Flow<List<ConversationMessage>> {
        return messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        model: String,
    ): Flow<NetworkResult<ConversationMessage>> {
        val settings = settingsDao.getLastSettings()
        // Сохраняем сообщение пользователя
        val userEntity = MessageEntity(
            conversationId = conversationId,
            role = "user",
            text = message,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userEntity)

        // Получаем всю историю
        val allMessages = messageDao.getMessagesByConversationSync(conversationId)
            .map { it.toModel() }

        // Отправляем в API
        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                temperature = settings?.temperature ?: 0.1,
                maxTokens = settings?.maxTokens ?: 500
            ),
            messages = allMessages.map {
                YaMessageRequest(role = it.role.title, text = it.text)
            }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            with(response) {
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
                    timestamp = System.currentTimeMillis()
                )

                val assistantId = messageDao.insertMessage(assistantEntity)

                ConversationMessage(
                    id = assistantId,
                    conversationId = conversationId,
                    role = Role.ASSISTANT,
                    text = parsed.answer.orEmpty(),
                    timestamp = assistantEntity.timestamp,
                    isContinue = parsed.isCOntinue == true,
                    isComplete = parsed.isComplete == true
                )

            }
        }

    }

    override suspend fun clearConversation(conversationId: String, initNew: Boolean) {
        messageDao.clearConversation(conversationId)
        if(initNew) initializeConversation(conversationId)
    }

    override suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversation(conversationId)
    }

    override fun getAllConversations(): Flow<List<String>> {
        return messageDao.getAllConversations()
    }

    private fun parseConversationState(text: String): ConversationState {
        return when {
            text.contains("[STATUS:COMPLETE]", ignoreCase = true) -> ConversationState(
                isComplete = true,
                finalResult = text.replace(
                    Regex("\\[STATUS:COMPLETE]", RegexOption.IGNORE_CASE),
                    ""
                ).trim()
            )

            else -> ConversationState(isComplete = false)
        }
    }
}