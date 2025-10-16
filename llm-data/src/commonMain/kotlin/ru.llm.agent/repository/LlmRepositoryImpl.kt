package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.api.YandexApi
import ru.llm.agent.data.request.CompletionOptions
import ru.llm.agent.data.request.YaMessageRequest
import ru.llm.agent.data.request.YaRequest
import ru.llm.agent.data.response.YandexGPTResponse
import ru.llm.agent.mapNetworkResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.toModel
import ru.llm.agent.utils.handleApi

public class LlmRepositoryImpl(
    private val yandexApi: YandexApi
) : LlmRepository {

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
                    this.result.usage.completionTokens,
                    outputFormat

                )
            }
        }
    }
}