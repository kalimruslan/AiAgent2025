package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.RoleSender
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat

public interface LlmRepository {

    public suspend fun sendMessageToProxyApi(
        roleSender: String = RoleSender.USER.type,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>>

    public suspend fun sendMessageToYandexGPT(
        promptMessage: MessageModel.PromtMessage?,
        userMessage: MessageModel.UserMessage,
        model: String,
        outputFormat: PromtFormat
    ): Flow<NetworkResult<MessageModel?>>
}