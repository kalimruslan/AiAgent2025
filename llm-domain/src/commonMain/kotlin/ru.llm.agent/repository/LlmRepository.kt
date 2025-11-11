package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.RoleSender
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.conversation.MessageWithTokensModels
import ru.llm.agent.model.mcp.YaGptTool

/**
 * Репозиторий для работы с различными LLM провайдерами.
 * Предоставляет низкоуровневый API для взаимодействия с YandexGPT и ProxyAPI.
 */
public interface LlmRepository {

    /**
     * Подсчитать количество токенов в тексте для YandexGPT
     *
     * @param text Текст для подсчета токенов
     * @param modelUri URI модели, null = использовать модель по умолчанию
     * @return Flow с результатом подсчета токенов
     */
    public suspend fun countYandexGPTTokens(text: String, modelUri: String? = null): Flow<NetworkResult<Int>>

    /**
     * Суммаризировать текст с помощью YandexGPT
     *
     * @param text Текст для суммаризации
     * @param model ID модели для использования
     * @param maxTokens Максимальное количество токенов в ответе
     * @return Суммаризированный текст
     */
    public suspend fun summarizeYandexGPTText(text: String, model: String, maxTokens: Int = 500): String

    /**
     * Отправить сообщение через ProxyAPI (OpenAI/OpenRouter)
     *
     * @param roleSender Роль отправителя (user/system/assistant)
     * @param text Текст сообщения
     * @param model ID модели для использования
     * @return Flow с результатом отправки сообщения
     */
    public suspend fun sendMessageToProxyApi(
        roleSender: String = RoleSender.USER.type,
        text: String,
        model: String
    ): Flow<NetworkResult<MessageModel?>>

    /**
     * Отправить сообщение в YandexGPT с промптом и пользовательским сообщением
     *
     * @param promptMessage Системный промпт (может быть null)
     * @param userMessage Сообщение пользователя
     * @param model ID модели для использования
     * @param outputFormat Формат вывода (text/json)
     * @return Flow с результатом отправки сообщения
     */
    public suspend fun sendMessageToYandexGPT(
        promptMessage: MessageModel.PromtMessage?,
        userMessage: MessageModel.UserMessage,
        model: String,
        outputFormat: PromtFormat
    ): Flow<NetworkResult<MessageModel?>>

    /**
     * Отправить множество сообщений в YandexGPT
     *
     * @param messages Список сообщений в виде map (role -> text)
     * @return Flow с результатом и информацией о токенах
     */
    public suspend fun sendMessagesToYandexGpt(
        messages: List<Map<String, String>>
    ): Flow<NetworkResult<MessageWithTokensModels?>>

    /**
     * Отправить сообщения в YandexGPT с поддержкой MCP tools
     *
     * @param messages Список сообщений
     * @param availableTools Список доступных инструментов для function calling
     * @return Flow с результатом отправки сообщения
     */
    public suspend fun sendMessagesToYandexGptWithMcp(
        messages: List<MessageModel>, availableTools: List<YaGptTool>
    ): Flow<NetworkResult<MessageModel?>>
}