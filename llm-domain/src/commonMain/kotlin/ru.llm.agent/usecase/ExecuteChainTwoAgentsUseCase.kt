package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import ru.llm.agent.NetworkResult
import ru.llm.agent.RoleSender
import ru.llm.agent.model.AgentsChainResultModel
import ru.llm.agent.model.AssistantJsonAnswer
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.repository.LlmRepository

/**
 * UseCase для выполнения цепочки двух агентов(первый дает результат, а второй на основе него делает что-то)
 */
public class ExecuteChainTwoAgentsUseCase(
    private val llmRepository: LlmRepository
) {

    public suspend operator fun invoke(
        initialTask: MessageModel.UserMessage
    ): Flow<NetworkResult<AgentsChainResultModel>> = flow {
        var agentsChainResultModel = AgentsChainResultModel()
        llmRepository.sendMessageToProxyApi(
            roleSender = RoleSender.USER.type,
            text = initialTask.content,
            model = "gpt-4o-mini"
        ).collect {
            when (it) {
                is NetworkResult.Error -> emit(it as NetworkResult<AgentsChainResultModel>)
                is NetworkResult.Loading -> emit(it as NetworkResult<AgentsChainResultModel>)
                is NetworkResult.Success -> {
                    val parsed =
                        Json.decodeFromString<AssistantJsonAnswer>((it.data as MessageModel.ResponseMessage).content)

                    agentsChainResultModel = agentsChainResultModel.copy(
                        firstAgentMessage = parsed.answer.orEmpty()
                    )

                    val promtMessage = MessageModel.UserMessage(
                        role = Role.USER,
                        content = """
                    Ты аналитик данных. Получен результат вычисления ${initialTask.content} от первого агента:
                    ${parsed.answer.orEmpty()}

                    Твоя задача:
                            1. Проанализируй результат
                            2. Создай практический пример использования этого результата
                            3. Дай рекомендации

                            Ответь в формате:
                            📊 АНАЛИЗ:
                            [твой анализ]

                            💡 ПРАКТИЧЕСКИЙ ПРИМЕР:
                            [пример использования]

                            ✅ РЕКОМЕНДАЦИИ:
                            [рекомендации]
                """.trimIndent()
                    )

                    llmRepository.sendMessageToYandexGPT(
                        promptMessage = null,
                        userMessage = promtMessage,
                        model = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
                        outputFormat = PromtFormat.TEXT
                    ).collect {
                        when (it) {
                            is NetworkResult.Error -> emit(it as NetworkResult<AgentsChainResultModel>)
                            is NetworkResult.Loading -> emit(
                                NetworkResult.Success(
                                    agentsChainResultModel
                                )
                            )

                            is NetworkResult.Success -> {
                                agentsChainResultModel = agentsChainResultModel.copy(
                                    secondAgentMessage = (it.data as MessageModel.ResponseMessage).content
                                )
                                emit(NetworkResult.Success(agentsChainResultModel))
                            }
                        }
                    }
                }
            }
        }
    }
}