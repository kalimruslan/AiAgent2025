package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
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
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.MessageWithTokensModels
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.config.LlmConfig
import ru.llm.agent.model.config.LlmTaskType
import ru.llm.agent.model.mcp.YaGptTool
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi
import java.util.logging.Logger
import kotlin.collections.map

public class LlmRepositoryImpl(
    private val proxyApi: ProxyApi,
    private val yandexApi: YandexApi,
    private val llmConfigRepository: LlmConfigRepository,
) : LlmRepository {

    override suspend fun countYandexGPTTokens(
        text: String,
        modelUri: String?,
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
            it.tokens?.size ?: 0
        }
    }

    // Сжатие текста через summarization
    override suspend fun summarizeYandexGPTText(
        text: String,
        model: String,
        maxTokens: Int,
    ): String {
        // Используем предустановленную конфигурацию для суммаризации
        val config = LlmConfig.summarizationYandexGpt().withMaxTokens(maxTokens)

        var summarizedText = ""
        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                stream = config.stream,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            ),
            messages = listOf(
                YaMessageRequest(
                    Role.SYSTEM.title,
                    config.systemPrompt ?: "Ты помощник, который кратко суммирует текст, сохраняя ключевую информацию."
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
        model: String,
    ): Flow<NetworkResult<MessageModel?>> {
        // Используем конфигурацию для GPT-4o-mini по умолчанию
        val config = LlmConfig.defaultProxyApiGpt4o()

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
            temperature = config.temperature,
            maxTokens = config.maxTokens
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
        outputFormat: PromtFormat,
    ): Flow<NetworkResult<MessageModel?>> {
        // Используем конфигурацию по умолчанию для YandexGPT
        val config = LlmConfig.defaultYandexGpt()

        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = config.stream
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
        // Используем конфигурацию для анализа цепочки агентов
        val config = LlmConfig.chainAnalysisYandexGpt()

        val request = YaRequest(
            modelUri = config.modelId,
            completionOptions = CompletionOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = config.stream
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

    override suspend fun sendMessagesToYandexGptWithMcp(
        messages: List<MessageModel>,
        availableTools: List<YaGptTool>,
    ): Flow<NetworkResult<MessageModel?>> {
        // Используем конфигурацию по умолчанию для YandexGPT (подходит для MCP чата)
        val config = LlmConfig.defaultYandexGpt()

        val request = YaRequest(
            modelUri = config.modelId,
            completionOptions = CompletionOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = config.stream
            ),
            messages = messages.map {
                YaMessageRequest(role = it.role.title, text = it.text)
            },
            tools = availableTools.ifEmpty { null }
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            Logger.getLogger("McpClient").info("Response sendMessagesToYandexGptWithMcp: ${response}")
            response.result.alternatives.firstOrNull()?.message?.toModel()
        }
    }
}