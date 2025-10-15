package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.OutputFormat
import ru.llm.agent.RoleSender
import ru.llm.agent.model.MessageModel
import ru.llm.agent.repository.LlmRepository

public class SendMessageToYandexGpt (
    private val repository: LlmRepository
){
    public suspend operator fun invoke(
        message: String,
        roleSender: RoleSender,
        outputFormat: OutputFormat,
        model: String
    ) : Flow<NetworkResult<MessageModel?>>{
        return repository.sendMessageToYandexGPT(
            roleSender = roleSender.type,
            text = message,
            model = model
        )
    }
}