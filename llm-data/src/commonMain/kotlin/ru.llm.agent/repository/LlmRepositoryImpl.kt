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
import ru.llm.agent.data.response.yaGPT.YandexGPTResponse
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi

public class LlmRepositoryImpl(
    private val proxyApi: ProxyApi,
    private val yandexApi: YandexApi
) : LlmRepository {

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
                Отвечай кратко, только текстом, без форматирования.
                Не используй markdown, списки или специальные символы.
                Максимум 2-3 предложения.
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
        promptMessage: MessageModel.PromtMessage,
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
            messages = listOf(
                YaMessageRequest(
                    promptMessage.role.title,
                    promptMessage.text
                ),
                YaMessageRequest(userMessage.role.title, userMessage.content)
            )
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            with(response) {
                this.result.alternatives.firstOrNull()?.message?.toModel(
                    this.result.usage?.totalTokens,
                    outputFormat
                )
            }
        }
    }
}