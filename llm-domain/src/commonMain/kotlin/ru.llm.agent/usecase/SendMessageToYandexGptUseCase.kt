package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationState
import ru.llm.agent.repository.LlmRepository

public class SendMessageToYandexGptUseCase(
    private val repository: LlmRepository
) {
    public suspend operator fun invoke(
        userMessage: MessageModel.UserMessage,
        outputFormat: PromtFormat,
        model: String
    ): Flow<NetworkResult<MessageModel?>> {

        val systemPromptText = when (outputFormat) {
            PromtFormat.JSON -> """
                Отвечай строго в JSON формате по следующей схеме:
                {
                  "answer": "текст ответа",
                  "tokens": "число токенов, например: 12345",
                  "model_version": "версия модели, напрример 25.03.2025",
                }
                
                В поле confidence вставь значение totalTokens из usage модели.
                В поле model_version вставь значение modelVersion из result модели.
                Не добавляй никакого текста до или после JSON.
                """.trimIndent()

            PromtFormat.TEXT -> """
                Отвечай кратко, только текстом, без форматирования.
                Не используй markdown, списки или специальные символы.
                Максимум 2-3 предложения.
                """.trimIndent()
            PromtFormat.MARKDOWN -> """
                Используй markdown для форматирования ответа.
                Применяй заголовки, списки, выделение текста где уместно.
            """.trimIndent()
        }

        val promptMessage = MessageModel.PromtMessage(
            role = Role.SYSTEM,
            text = systemPromptText
        )


        return repository.sendMessageToYandexGPT(
            promptMessage = promptMessage,
            outputFormat = outputFormat,
            userMessage = userMessage,
            model = model
        )
    }
}