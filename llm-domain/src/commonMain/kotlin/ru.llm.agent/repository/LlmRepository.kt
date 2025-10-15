package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.RoleSender
import ru.llm.agent.model.MessageModel

public interface LlmRepository {
    public suspend fun sendMessageToYandexGPT(
        messageModel: MessageModel,
        model: String
    ): Flow<NetworkResult<MessageModel?>>
}