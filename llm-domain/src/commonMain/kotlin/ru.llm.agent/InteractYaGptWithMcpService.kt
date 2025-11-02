package ru.llm.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.Role
import ru.llm.agent.model.mcp.FunctionResult
import ru.llm.agent.model.mcp.ToolResult
import ru.llm.agent.model.mcp.ToolResultList
import ru.llm.agent.model.mcp.YaGptTool
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.McpRepository
import java.util.logging.Logger
import kotlin.let

public class InteractYaGptWithMcpService(
    private val mcpRepository: McpRepository,
    private val llmRepository: LlmRepository,
) {
    private val conversationHistory = mutableListOf<MessageModel>()
    private var availableTools: List<YaGptTool> = emptyList()

    public fun getTools(): Flow<List<YaGptTool>> {
        return flow {
            // Получаем список инструментов с MCP сервера
            availableTools = mcpRepository.getMcpToolsList()
            emit(availableTools)
        }

    }

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
                            Logger.getLogger("McpClient").info("Error: ${result.message}")
                            emit(NetworkResult.Error(result.message))
                        }

                        is NetworkResult.Success -> {
                            val message = result.data
                            Logger.getLogger("McpClient").info("Received message: $message")
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
                                        Logger.getLogger("McpClient")
                                            .info("chat toolCall - $toolCall")

                                        try {
                                            val arguments = toolCall.functionCall.arguments
                                            Logger.getLogger("McpClient")
                                                .info("chat arguments - $arguments")

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
                                            Logger.getLogger("McpClient")
                                                .info("call tool - $toolResults")

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

                                    // Добавляем результаты вызовов в историю
//                            conversationHistory.add(
//                                MessageModel.ToolsMessage(
//                                    role = Role.FUNCTION,
//                                    toolResultList = ToolResultList(toolResults = toolResults)
//                                )
//                            )
                                }
                            } ?: emit(NetworkResult.Error("Error"))
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