package ru.llm.agent.service

import kotlinx.coroutines.flow.Flow
import ru.ai.agent.data.request.proxyapi.ProxyApiRequest
import ru.ai.agent.data.request.proxyapi.ProxyMessageRequest
import ru.ai.agent.data.response.proxyapi.ProxyApiResponse
import ru.llm.agent.NetworkResult
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.api.YandexApi
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.data.request.yaGPT.CompletionOptions
import ru.llm.agent.data.request.yaGPT.YaMessageRequest
import ru.llm.agent.data.request.yaGPT.YaRequest
import ru.llm.agent.data.response.yaGPT.YandexGPTResponse
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.usecase.ParseAssistantResponseUseCase
import ru.llm.agent.utils.handleApi

/**
 * Реализация сервиса отправки сообщений через различные LLM провайдеры.
 *
 * @param yandexApi API клиент для Yandex GPT
 * @param proxyApi API клиент для Proxy API (OpenAI, Mistral и т.д.)
 * @param parseAssistantResponseUseCase Use case для парсинга JSON ответов
 * @param logger Кроссплатформенный логгер
 */
public class MessageSendingServiceImpl(
    private val yandexApi: YandexApi,
    private val proxyApi: ProxyApi,
    private val parseAssistantResponseUseCase: ParseAssistantResponseUseCase,
    private val logger: Logger,
) : MessageSendingService {

    /**
     * Отправить сообщения через указанный провайдер
     */
    override suspend fun sendMessages(
        conversationId: String,
        messages: List<ConversationMessage>,
        provider: LlmProvider,
        temperature: Double?,
        maxTokens: Int?
    ): Flow<NetworkResult<MessageSendingResult>> {
        return when (provider) {
            LlmProvider.YANDEX_GPT -> sendToYandex(conversationId, messages, provider, temperature, maxTokens)
            LlmProvider.PROXY_API_GPT4O_MINI,
            LlmProvider.PROXY_API_MISTRAY_AI -> sendToProxy(conversationId, messages, provider, temperature, maxTokens)
        }
    }

    /**
     * Отправка сообщений через Yandex GPT
     */
    private suspend fun sendToYandex(
        conversationId: String,
        messages: List<ConversationMessage>,
        provider: LlmProvider,
        temperature: Double?,
        maxTokens: Int?
    ): Flow<NetworkResult<MessageSendingResult>> {
        val request = YaRequest(
            modelUri = provider.modelId,
            completionOptions = CompletionOptions(
                temperature = temperature ?: 0.1,
                maxTokens = maxTokens ?: 500
            ),
            messages = messages.map {
                YaMessageRequest(role = it.role.title, text = it.text)
            }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            val rawResponse = response.result.alternatives.firstOrNull()?.message?.text
                ?: throw Exception("Empty response from Yandex API")

            parseAndCreateMessage(conversationId, rawResponse, provider, "Yandex")
        }
    }

    /**
     * Отправка сообщений через Proxy API (OpenAI, Mistral и т.д.)
     */
    private suspend fun sendToProxy(
        conversationId: String,
        messages: List<ConversationMessage>,
        provider: LlmProvider,
        temperature: Double?,
        maxTokens: Int?
    ): Flow<NetworkResult<MessageSendingResult>> {
        val request = ProxyApiRequest(
            model = provider.modelId,
            temperature = temperature ?: 0.7,
            maxTokens = maxTokens ?: 500,
            messages = messages.map {
                ProxyMessageRequest(role = it.role.title, text = it.text)
            }
        )

        val result = handleApi<ProxyApiResponse> {
            proxyApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: ProxyApiResponse ->
            val rawResponse = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("Empty response from Proxy API")

            parseAndCreateMessage(conversationId, rawResponse, provider, "ProxyAPI")
        }
    }

    /**
     * Общий метод для парсинга ответа и создания сообщения.
     * Устраняет дублирование кода между sendToYandex и sendToProxy.
     *
     * @param conversationId ID диалога
     * @param rawResponse Сырой ответ от API
     * @param provider LLM провайдер
     * @param providerName Имя провайдера для логирования
     * @return Результат отправки сообщения
     */
    private fun parseAndCreateMessage(
        conversationId: String,
        rawResponse: String,
        provider: LlmProvider,
        providerName: String
    ): MessageSendingResult {
        // Парсим ответ через use case
        val parseResult = parseAssistantResponseUseCase(rawResponse)
        val parsed = parseResult.getOrElse {
            logger.error("Ошибка парсинга ответа от $providerName: ${it.message}")
            throw it
        }

        // Формируем сообщение для сохранения
        val conversationMessage = ConversationMessage(
            id = 0L, // ID будет присвоен при сохранении в БД
            conversationId = conversationId,
            role = Role.ASSISTANT,
            text = parsed.answer.orEmpty(),
            timestamp = System.currentTimeMillis(),
            isContinue = parsed.isCOntinue == true,
            isComplete = parsed.isComplete == true,
            originalResponse = rawResponse,
            model = provider.displayName
        )

        return MessageSendingResult(
            conversationMessage = conversationMessage,
            rawResponse = rawResponse
        )
    }
}