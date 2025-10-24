package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
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
import ru.llm.agent.data.request.yaGPT.YaTokensRequest
import ru.llm.agent.data.response.yaGPT.YaTokenizerResponse
import ru.llm.agent.data.response.yaGPT.YandexGPTResponse
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.AssistantJsonAnswer
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.MessageWithTokensModels
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi
import java.util.logging.Logger
import kotlin.collections.map

public class LlmRepositoryImpl(
    private val proxyApi: ProxyApi,
    private val yandexApi: YandexApi
) : LlmRepository {

    override suspend fun countYandexGPTTokens(
        text: String,
        modelUri: String?
    ): Flow<NetworkResult<Int>> {
        val request = YaTokensRequest(
            modelUri = modelUri.orEmpty(),
            messages = listOf(
                YaMessageRequest(
                    Role.USER.title,
                    text = text
                )
            )
        )
        return handleApi<YaTokenizerResponse> {
            yandexApi.countTokens(request)
        }.mapNetworkResult { it: YaTokenizerResponse ->
            it.tokens?.size?:0
        }
    }

    // Сжатие текста через summarization
    override suspend fun summarizeYandexGPTText(
        text: String,
        model: String,
        maxTokens: Int
    ): String {
        var summarizedText = ""
        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                stream = false,
                temperature = 0.3,
                maxTokens = maxTokens
            ),
            messages = listOf(
                YaMessageRequest(
                    Role.SYSTEM.title,
                    "Ты помощник, который кратко суммирует текст, сохраняя ключевую информацию."
                ),
                YaMessageRequest(
                    Role.USER.title,
                    "Сократи следующий текст, сохранив главное:\\n\\n$text"
                )
            )
        )

        handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }.collect {
            if (it is NetworkResult.Success) {
                summarizedText = it.data.result.alternatives.firstOrNull()?.message?.text.orEmpty()
            } else {
                summarizedText = ""
            }
        }

        return summarizedText
    }

    override suspend fun sendMessageToProxyApi(
        roleSender: String,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>> {
        val request = ProxyApiRequest(
            model = model,
            messages = listOf(
                ProxyMessageRequest(
                    "system",
                    """
                        Отвечай строго в JSON формате по следующей схеме:
                            {
                              "answer": "текст ответа"
                            }
                        Не добавляй никакого текста до или после JSON.
                """.trimIndent()
                ),
                ProxyMessageRequest("user", text)
            ),
            temperature = 0.7,
            maxTokens = 1024
        )
        val result = handleApi<ProxyApiResponse> {
            proxyApi.sendMessage(request)
        }

        return result.mapNetworkResult { proxyApiResponse: ProxyApiResponse ->
            with(proxyApiResponse) {
                choices.firstOrNull()?.message?.toModel(usage?.totalTokens.toString())
            }
        }
    }

    override suspend fun sendMessageToYandexGPT(
        promptMessage: MessageModel.PromtMessage?,
        userMessage: MessageModel.UserMessage,
        model: String,
        outputFormat: PromtFormat
    ): Flow<NetworkResult<MessageModel?>> {
        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                temperature = 0.6,
                maxTokens = 2000
            ),
            messages = if (promptMessage == null) {
                listOf(
                    YaMessageRequest(userMessage.role.title, userMessage.content)
                )
            } else {
                listOf(
                    YaMessageRequest(
                        promptMessage.role.title,
                        promptMessage.text
                    ),
                    YaMessageRequest(userMessage.role.title, userMessage.content)
                )
            }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            with(response) {
                this.result.alternatives.firstOrNull()?.message?.toModel(
                    this.result.usage?.completionTokens,
                    outputFormat
                )
            }
        }
    }

    override suspend fun sendMessagesToYandexGpt(messages: List<Map<String, String>>): Flow<NetworkResult<MessageWithTokensModels?>> {
        val request = YaRequest(
            modelUri = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
            completionOptions = CompletionOptions(
                temperature = 0.3,
                maxTokens = 500
            ),
            messages = messages.flatMap { map ->
                map.map {
                    YaMessageRequest(role = it.key, text = it.value)
                }
            }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            with(response) {
                val messageText =
                    response.result.alternatives.firstOrNull()?.message?.text

                MessageWithTokensModels(
                    role = Role.ASSISTANT.title,
                    text = messageText.orEmpty(),
                    tokens = response.result.usage?.totalTokens?.toInt() ?: 0
                )

            }
        }

    }
}