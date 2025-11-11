package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.repository.ExpertRepository

/**
 * Use case для синтеза мнений экспертов в единый финальный ответ.
 *
 * Логика:
 * 1. Получает список мнений экспертов
 * 2. Формирует промпт с вопросом и всеми мнениями
 * 3. Отправляет через SendMessageWithCustomPromptUseCase с промптом синтеза
 * 4. Сохраняет результат синтеза как мнение специального "эксперта"
 * 5. Возвращает синтезированный ответ
 */
public class SynthesizeExpertOpinionsUseCase(
    private val sendMessageWithCustomPromptUseCase: SendMessageWithCustomPromptUseCase,
    private val expertRepository: ExpertRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val logger: Logger
) {
    /**
     * Синтезировать финальный ответ на основе мнений экспертов
     *
     * @param conversationId ID диалога
     * @param userMessageId ID сообщения пользователя (для привязки синтеза)
     * @param userQuestion Вопрос пользователя
     * @param expertOpinions Список мнений экспертов
     * @param provider LLM провайдер для синтеза
     * @return Flow с результатом синтеза
     */
    public suspend operator fun invoke(
        conversationId: String,
        userMessageId: Long,
        userQuestion: String,
        expertOpinions: List<ExpertOpinionResult>,
        provider: LlmProvider
    ): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())

        if (expertOpinions.isEmpty()) {
            emit(NetworkResult.Error(
                DomainError.ValidationError(
                    field = "expertOpinions",
                    message = "Нет мнений экспертов для синтеза"
                )
            ))
            return@flow
        }

        try {
            // Формируем системный промпт для синтеза
            val synthesisSystemPrompt = systemPromptBuilder.buildSynthesisPrompt(expertOpinions.size)

            // Формируем user message с вопросом и мнениями
            val opinionsData = expertOpinions.map {
                SystemPromptBuilder.ExpertOpinionData(
                    name = it.expert.name,
                    opinion = it.opinion,
                    icon = it.expert.icon
                )
            }
            val synthesisUserPrompt = systemPromptBuilder.buildUserQuestionWithOpinions(
                userQuestion = userQuestion,
                expertOpinions = opinionsData
            )

            logger.info("Synthesis system prompt: $synthesisSystemPrompt\n\nUser prompt: $synthesisUserPrompt")

            // Создаем временный диалог для синтеза
            val synthesisConversationId = "$conversationId-synthesis"

            // Отправляем запрос на синтез
            sendMessageWithCustomPromptUseCase(
                conversationId = synthesisConversationId,
                userMessage = synthesisUserPrompt,
                systemPrompt = synthesisSystemPrompt,
                provider = provider
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val finalAnswer = result.data.text
                        logger.info("Synthesis SUCCESS: $finalAnswer")

                        // Сохраняем синтез как мнение специального "эксперта"
                        expertRepository.saveExpertOpinion(
                            expertId = "synthesis",
                            expertName = "Синтез",
                            expertIcon = "🎯",
                            messageId = userMessageId,
                            conversationId = conversationId,
                            opinion = finalAnswer,
                            timestamp = System.currentTimeMillis(),
                            originalResponse = result.data.text
                        )

                        emit(NetworkResult.Success(finalAnswer))
                    }
                    is NetworkResult.Error -> {
                        emit(NetworkResult.Error(
                            DomainError.BusinessLogicError(
                                reason = "synthesis_failed",
                                message = "Ошибка при синтезе: ${result.error.toUserMessage()}"
                            )
                        ))
                    }
                    is NetworkResult.Loading -> {
                        // Промежуточное состояние
                        emit(NetworkResult.Loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(
                DomainError.UnknownError(
                    message = "Ошибка при синтезе финального ответа: ${e.message}",
                    exception = e
                )
            ))
        }
    }
}
