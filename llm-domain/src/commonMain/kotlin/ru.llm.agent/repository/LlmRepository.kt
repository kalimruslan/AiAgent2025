package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.RoleSender
import ru.llm.agent.model.MessageModel

public interface LlmRepository {

    public suspend fun sendMessageToProxyApi(
        roleSender: String = RoleSender.USER.type,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>>

    public suspend fun sendMessageToYandexGPT(
        roleSender: String = RoleSender.USER.type,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>>
}