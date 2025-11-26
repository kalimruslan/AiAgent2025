package ru.llm.agent.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.model.mcp.FunctionResult
import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.model.mcp.ToolResult
import ru.llm.agent.model.mcp.ToolResultList
import ru.llm.agent.model.mcp.YaGptFunction
import ru.llm.agent.model.mcp.YaGptTool
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.toYaGptTool

/**
 * Use case для чата с поддержкой MCP tool calling.
 *
 * Реализует полный цикл function calling:
 * 1. Получает список доступных инструментов с MCP сервера
 * 2. Сохраняет сообщение пользователя в БД
 * 3. Конвертирует ConversationMessage в MessageModel
 * 4. Отправляет сообщение в LLM с tool definitions
 * 5. Обрабатывает tool calls от модели
 * 6. Выполняет инструменты через MCP
 * 7. Отправляет результаты обратно в модель
 * 8. Повторяет шаги 5-7 до получения финального ответа (макс. 3 итерации)
 *
 * @param conversationRepository Репозиторий для работы с диалогами
 * @param llmRepository Репозиторий для работы с LLM
 * @param mcpRepository Репозиторий для работы с MCP инструментами
 * @param logger Логгер для отладки
 */
public class ChatWithMcpToolsUseCase(
    private val conversationRepository: ConversationRepository,
    private val llmRepository: LlmRepository,
    private val mcpRepository: McpRepository,
    private val logger: Logger,
) {
    /**
     * Отправить сообщение с поддержкой MCP tool calling
     *
     * @param conversationId ID диалога
     * @param message Сообщение пользователя
     * @param provider Провайдер LLM (должен быть YANDEX_GPT для MCP)
     * @param maxIterations Максимальное количество итераций tool calling (по умолчанию 3)
     * @return Flow с результатами обработки (промежуточные и финальный)
     */
    public suspend operator fun invoke(
        conversationId: String,
        message: String,
        provider: LlmProvider = LlmProvider.YANDEX_GPT,
        maxIterations: Int = 3,
        needAddToHistory: Boolean = true,
        availableTools: List<McpToolInfo> = emptyList(),
    ): Flow<NetworkResult<ConversationMessage>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Валидация: не отправляем пустые сообщения
            if (message.isBlank()) {
                emit(
                    NetworkResult.Error(
                        DomainError.ValidationError(
                            field = "message",
                            message = "Сообщение не может быть пустым"
                        )
                    )
                )
                return@flow
            }

            // Проверяем, что провайдер поддерживает MCP
            if (provider != LlmProvider.YANDEX_GPT) {
                emit(
                    NetworkResult.Error(
                        DomainError.ValidationError(
                            field = "provider",
                            message = "MCP поддерживается только для YandexGPT"
                        )
                    )
                )
                return@flow
            }

            // 1. Получаем доступные инструменты
            val availableTools = availableTools.map {
                it.toYaGptTool()
            }.ifEmpty { mcpRepository.getMcpToolsList() }
            logger.info("Получено ${availableTools.size} MCP инструментов")

            // 2. Сохраняем сообщение пользователя
            val messageId =
                conversationRepository.saveUserMessage(conversationId, message, provider)

            // 3. Получаем историю диалога и конвертируем в MessageModel
            val conversationMessages =
                conversationRepository.getMessagesByConversationSync(conversationId)
                    .filter { it.role == Role.USER }
            val messageHistory = convertToMessageModels(conversationMessages)

            if (!needAddToHistory) {
                conversationRepository.deleteMessageById(messageId)
            }

            // 4. Запускаем итеративный процесс tool calling
            var iteration = 0
            var currentHistory = messageHistory.toMutableList()
            var shouldContinue = true

            while (iteration < maxIterations && shouldContinue) {
                iteration++
                logger.info("MCP итерация $iteration/$maxIterations")

                // Отправляем сообщение в LLM с tool definitions
                llmRepository.sendMessagesToYandexGptWithMcp(
                    messages = currentHistory,
                    availableTools = availableTools
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val responseMessage = result.data

                            if (responseMessage == null) {
                                emit(
                                    NetworkResult.Error(
                                        DomainError.UnknownError(
                                            message = "Получен пустой ответ от LLM"
                                        )
                                    )
                                )
                                shouldContinue = false
                                return@collect
                            }

                            when (responseMessage) {
                                is MessageModel.ResponseMessage -> {
                                    // Добавляем ответ в историю
                                    if (responseMessage.content.isNotEmpty()) {
                                        currentHistory.add(responseMessage)
                                    }

                                    // Проверяем, есть ли tool calls
                                    val toolCalls = responseMessage.toolCallList?.toolCalls

                                    if (toolCalls.isNullOrEmpty()) {
                                        // Финальный ответ без tool calls
                                        logger.info("Получен финальный ответ от LLM")

                                        // Конвертируем в ConversationMessage и сохраняем
                                        val conversationMessage = convertToConversationMessage(
                                            conversationId = conversationId,
                                            responseMessage = responseMessage,
                                            provider = provider
                                        )

                                        val savedId =
                                            if (needAddToHistory) conversationRepository.saveAssistantMessage(
                                                conversationMessage
                                            ) else 0

                                        emit(
                                            if (savedId > 0) {
                                                NetworkResult.Success(
                                                    conversationMessage.copy(id = savedId)
                                                )
                                            } else {
                                                NetworkResult.Success(
                                                    conversationMessage
                                                )
                                            }
                                        )
                                        shouldContinue = false
                                    } else {
                                        val toolResults = mutableListOf<ToolResult>()

                                        for (toolCall in toolCalls) {
                                            try {
                                                val result = mcpRepository.callTool(
                                                    name = toolCall.functionCall.name,
                                                    arguments = toolCall.functionCall.arguments
                                                )

                                                toolResults.add(
                                                    ToolResult(
                                                        functionResult = FunctionResult(
                                                            name = toolCall.functionCall.name,
                                                            content = result
                                                        )
                                                    )
                                                )

                                                // Эмитим промежуточный результат для UI
                                                val toolMessage = convertToConversationMessage(
                                                    conversationId = conversationId,
                                                    responseMessage = responseMessage.copy(
                                                        content = "Выполнение инструмента: ${toolCall.functionCall.name}\nРезультат: $result"
                                                    ),
                                                    provider = provider,
                                                    isToolCall = true
                                                )
                                                emit(NetworkResult.Success(toolMessage))
                                                conversationRepository.saveAssistantMessage(
                                                    ConversationMessage(
                                                        conversationId = conversationId,
                                                        role = Role.ASSISTANT,
                                                        text = result,
                                                        model = provider.displayName
                                                    )
                                                )
                                                delay(5000)
                                                // Добавляем результаты tools в историю
                                                currentHistory.add(
                                                    MessageModel.ToolsMessage(
                                                        role = Role.USER,
                                                        toolResultList = ToolResultList(toolResults),
                                                        text = buildString {
                                                            append("Результаты выполнения инструмента:")
                                                            appendLine("${toolCall.functionCall.name}: $result")
                                                        }
                                                    )
                                                )

                                            } catch (e: Exception) {
                                                logger.info("Ошибка при выполнении tool ${toolCall.functionCall.name}: ${e.message}")
                                                toolResults.add(
                                                    ToolResult(
                                                        functionResult = FunctionResult(
                                                            name = toolCall.functionCall.name,
                                                            content = "Ошибка: ${e.message}"
                                                        )
                                                    )
                                                )
                                            }
                                        }

                                        // Продолжаем итерацию
                                    }
                                }

                                else -> {
                                    logger.info("Неожиданный тип ответа: ${responseMessage::class.simpleName}")
                                }
                            }
                        }

                        is NetworkResult.Error -> {
                            emit(NetworkResult.Error(result.error))
                            shouldContinue = false
                        }

                        is NetworkResult.Loading -> {
                            emit(NetworkResult.Loading())
                        }
                    }
                }
            }

            // Проверяем, завершили ли успешно
            if (shouldContinue) {
                // Достигнут лимит итераций
                logger.info("Достигнуто максимальное количество итераций MCP: $maxIterations")
                emit(
                    NetworkResult.Error(
                        DomainError.UnknownError(
                            message = "Достигнуто максимальное количество итераций tool calling: $maxIterations"
                        )
                    )
                )
            }

        } catch (e: Exception) {
            emit(
                NetworkResult.Error(
                    DomainError.UnknownError(
                        message = "Ошибка при чате с MCP: ${e.message}",
                        exception = e
                    )
                )
            )
        }
    }

    /**
     * Конвертирует ConversationMessage в MessageModel для отправки в LLM
     */
    private fun convertToMessageModels(messages: List<ConversationMessage>): List<MessageModel> {
        return messages.map { message ->
            when (message.role) {
                Role.SYSTEM -> MessageModel.PromtMessage(
                    role = Role.SYSTEM,
                    text = message.text
                )

                Role.USER -> MessageModel.UserMessage(
                    role = Role.USER,
                    content = message.text
                )

                Role.ASSISTANT -> MessageModel.ResponseMessage(
                    role = Role.ASSISTANT,
                    content = message.text,
                    textFormat = PromtFormat.TEXT
                )

                else -> MessageModel.UserMessage(
                    role = Role.USER,
                    content = message.text
                )
            }
        }
    }

    /**
     * Конвертирует MessageModel.ResponseMessage в ConversationMessage для сохранения в БД
     */
    private fun convertToConversationMessage(
        conversationId: String,
        responseMessage: MessageModel.ResponseMessage,
        provider: LlmProvider,
        isToolCall: Boolean = false,
    ): ConversationMessage {
        return ConversationMessage(
            conversationId = conversationId,
            role = Role.ASSISTANT,
            text = responseMessage.content,
            model = provider.displayName,
            originalResponse = responseMessage.content,
            isContinue = isToolCall, // Используем isContinue для обозначения промежуточного результата tool
            isComplete = !isToolCall,
            inputTokens = responseMessage.inputTokens,
            completionTokens = responseMessage.completionTokens,
            totalTokens = responseMessage.totalTokens
        )
    }
}