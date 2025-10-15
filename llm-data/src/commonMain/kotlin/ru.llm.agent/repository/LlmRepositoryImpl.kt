package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.api.YandexApi
import ru.llm.agent.data.request.proxyapi.ProxyApiRequest
import ru.llm.agent.data.request.proxyapi.ProxyMessageRequest
import ru.llm.agent.data.request.yandexGPT.CompletionOptions
import ru.llm.agent.data.request.yandexGPT.YaMessageRequest
import ru.llm.agent.data.request.yandexGPT.YaRequest
import ru.llm.agent.data.response.proxyapi.ProxyApiResponse
import ru.llm.agent.data.response.yandexGPT.YandexGPTResponse
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.MessageModel
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
                    "Ты полезный AI ассистент. Отвечай кратко и по делу на русском языке."
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
                choices.firstOrNull()?.message?.toModel(usage?.totalTokens ?: 0)
            }
        }
    }

    override suspend fun sendMessageToYandexGPT(
        roleSender: String,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>> {
        val request = YaRequest(
            modelUri = model,
            completionOptions = CompletionOptions(
                temperature = 0.6,
                maxTokens = 2000
            ),
            messages = listOf(
                YaMessageRequest(
                    "system",
                    "Ты полезный AI ассистент. Отвечай кратко и по делу на русском языке."
                ),
                YaMessageRequest("user", text)
            )
        )

        val result = handleApi<YandexGPTResponse> {
            yandexApi.sendMessage(request)
        }

        return result.mapNetworkResult { response: YandexGPTResponse ->
            with(response) {
                this.result.alternatives.firstOrNull()?.message?.toModel(this.result.usage.completionTokens)
            }
        }
    }
}