package ru.llm.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.Role
import ru.llm.agent.model.mcp.FunctionResult
import ru.llm.agent.model.mcp.ToolResult
import ru.llm.agent.model.mcp.ToolResultList
import ru.llm.agent.model.mcp.YaGptTool
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.McpRepository
import kotlin.let

/**
 * Сервис для взаимодействия с YandexGPT через MCP (Model Context Protocol).
 * Реализует полный цикл function calling:
 * 1. Получение списка доступных инструментов с MCP сервера
 * 2. Отправка сообщения с tool definitions в YandexGPT
 * 3. Обработка tool calls от модели
 * 4. Выполнение инструментов через MCP
 * 5. Отправка результатов обратно в модель
 *
 * Поддерживает до 3 итераций tool calling для решения сложных задач.
 */
public class InteractYaGptWithMcpService(
    private val mcpRepository: McpRepository,
    private val llmRepository: LlmRepository,
    private val logger: Logger,
) {
    private val conversationHistory = mutableListOf<MessageModel>()
    private var availableTools: List<YaGptTool> = emptyList()

    /**
     * Получить список доступных MCP инструментов
     *
     * @return Flow со списком инструментов в формате YandexGPT
     */
    public fun getTools(): Flow<List<YaGptTool>> {
        return flow {
            // Получаем список инструментов с MCP сервера
            availableTools = mcpRepository.getMcpToolsList()
            emit(availableTools)
        }

    }

    /**
     * Отправить сообщение в YandexGPT с поддержкой MCP function calling
     *
     * Автоматически обрабатывает tool calls:
     * - Выполняет запрошенные инструменты
     * - Отправляет результаты обратно в модель
     * - Повторяет процесс до получения финального ответа (макс. 3 итерации)
     *
     * @param userMessage Сообщение пользователя
     * @return Flow с результатом обработки сообщения
     */
    public fun chat(userMessage: String): Flow<NetworkResult<MessageModel>> {
        return flow {
            conversationHistory.add(
                MessageModel.UserMessage(
                    role = Role.USER,
                    content = userMessage
                )
            )

            var maxIteration = 3
            while (maxIteration-- > 0) {
                llmRepository.sendMessagesToYandexGptWithMcp(
                    messages = conversationHistory,
                    availableTools = availableTools
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> emit(NetworkResult.Loading())
                        is NetworkResult.Error -> {
                            logger.info("Error: ${result.error.toLogMessage()}")
                            emit(NetworkResult.Error(result.error))
                        }

                        is NetworkResult.Success -> {
                            val message = result.data
                            logger.info("Received message: $message")
                            result.data?.let {
                                //conversationHistory.add(it)
                                val responseMessage = it as MessageModel.ResponseMessage

                                // Проверяем, хочет ли модель вызвать инструмент
                                val toolCalls = responseMessage.toolCallList?.toolCalls
                                if (toolCalls.isNullOrEmpty()) {
                                    // Модель вернула финальный ответ
                                    emit(NetworkResult.Success(it))
                                } else {
                                    // Выполняем вызовы инструментов
                                    val toolResults = mutableListOf<ToolResult>()

                                    for (toolCall in toolCalls) {
                                        logger.info("chat toolCall - $toolCall")

                                        try {
                                            val arguments = toolCall.functionCall.arguments
                                            logger.info("chat arguments - $arguments")

                                            val result = mcpRepository.callTool(
                                                name = toolCall.functionCall.name,
                                                arguments = arguments
                                            )

                                            val message = MessageModel.UserMessage(
                                                role = Role.USER,
                                                content = buildString {
                                                    append("Результаты выполнения инструмента:")
                                                    appendLine("${toolCall.functionCall.name}: $result")
                                                }
                                            )
                                            logger.info("call tool - $toolResults")

                                            conversationHistory.add(message)
                                            emit(NetworkResult.Success(message))


                                            toolResults.add(
                                                ToolResult(
                                                    functionResult = FunctionResult(
                                                        name = toolCall.functionCall.name,
                                                        content = result
                                                    )
                                                )
                                            )
                                        } catch (e: Exception) {
                                            toolResults.add(
                                                ToolResult(
                                                    functionResult = FunctionResult(
                                                        name = toolCall.functionCall.name,
                                                        content = "Error: ${e.message}"
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            } ?: emit(NetworkResult.Error(
                                DomainError.UnknownError(
                                    message = "Получен пустой ответ от LLM"
                                )
                            ))
                        }
                    }
                }
                emit(
                    NetworkResult.Success(
                        MessageModel.NoneMessage(
                            role = Role.NONE,
                            message = "Max iterations reached"
                        )
                    )
                )
            }
        }
    }

    public fun getHistory(): List<MessageModel> {
        return conversationHistory
    }

    public fun clearHistory() {
        conversationHistory.clear()
    }
}