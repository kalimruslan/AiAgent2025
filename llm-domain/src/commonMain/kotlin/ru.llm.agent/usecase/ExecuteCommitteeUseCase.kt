package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ExpertRepository

/**
 * UseCase для выполнения режима "Комитет экспертов"
 *
 * Координирует работу нескольких AI-экспертов:
 * 1. Получает мнение от каждого эксперта
 * 2. Сохраняет мнения в БД
 * 3. Синтезирует финальный ответ на основе всех мнений
 */
public class ExecuteCommitteeUseCase(
    private val conversationRepository: ConversationRepository,
    private val expertRepository: ExpertRepository,
    private val sendMessageWithCustomPromptUseCase: SendMessageWithCustomPromptUseCase,
    private val synthesizeExpertOpinionsUseCase: SynthesizeExpertOpinionsUseCase,
    private val logger: Logger,
) {

    /**
     * Выполнить обсуждение в комитете экспертов
     *
     * @param conversationId ID диалога
     * @param userMessage Сообщение пользователя
     * @param experts Список выбранных экспертов
     * @param provider LLM провайдер для общения с экспертами
     *
     * @return Flow с результатами: сначала мнения экспертов, затем финальный синтез
     */
    public suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        experts: List<Expert>,
        provider: LlmProvider
    ): Flow<NetworkResult<CommitteeResult>> = flow {
        emit(NetworkResult.Loading())

        logger.info("Execute comitte START - experts - ${experts.map { it.name }}")

        if (experts.isEmpty()) {
            emit(NetworkResult.Error("Не выбраны эксперты"))
            return@flow
        }

        // Сначала сохраняем сообщение пользователя и получаем его ID
        val userMessageId = conversationRepository.saveUserMessage(
            conversationId = conversationId,
            message = userMessage,
            provider = provider
        )

        if (userMessageId == 0L) {
            emit(NetworkResult.Error("Не удалось сохранить сообщение пользователя"))
            return@flow
        }

        logger.info("User message saved with ID: $userMessageId")

        val expertOpinions = mutableListOf<ExpertOpinionResult>()

        // 1. Собираем мнения от каждого эксперта
        for (expert in experts) {
            emit(NetworkResult.Loading())

            try {
                // Создаем временный диалог для эксперта с его системным промптом
                val expertConversationId = "$conversationId-expert-${expert.id}"

                // Инициализируем диалог для эксперта
                //conversationRepository.initializeConversation(expertConversationId)
                logger.info("Send message to expert - ${expert.name}, conversationId - $conversationId,\n" +
                        "systemPrompt - ${expert.systemPrompt}\n\nВопрос: $userMessage")
                // Получаем мнение эксперта с правильным разделением ролей
                var expertOpinion = ""
                sendMessageWithCustomPromptUseCase(
                    conversationId = expertConversationId,
                    userMessage = userMessage,
                    systemPrompt = expert.systemPrompt,
                    provider = provider
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            expertOpinion = result.data.text

                            // Сохраняем мнение эксперта в БД
                            expertRepository.saveExpertOpinion(
                                expertId = expert.id,
                                expertName = expert.name,
                                expertIcon = expert.icon,
                                messageId = userMessageId,
                                conversationId = conversationId,
                                opinion = expertOpinion,
                                timestamp = System.currentTimeMillis(),
                                originalResponse = result.data.text
                            )

                            val opinionResult = ExpertOpinionResult(
                                expert = expert,
                                opinion = expertOpinion
                            )
                            logger.info("Expert ${expert.name} opinion result - $opinionResult")
                            expertOpinions.add(opinionResult)

                            // Эмитим каждое мнение эксперта
                            emit(NetworkResult.Success(CommitteeResult.ExpertOpinion(opinionResult)))
                        }
                        is NetworkResult.Error -> {
                            emit(NetworkResult.Error("Ошибка при получении мнения от ${expert.name}: ${result.message}"))
                        }
                        is NetworkResult.Loading -> {
                            // Промежуточное состояние загрузки
                        }
                    }
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error("Ошибка при обработке мнения эксперта ${expert.name}: ${e.message}"))
            }
        }

        // 2. Синтезируем финальный ответ на основе всех мнений
        if (expertOpinions.isNotEmpty()) {
            emit(NetworkResult.Loading())

            try {
                synthesizeExpertOpinionsUseCase(
                    conversationId = conversationId,
                    userMessageId = userMessageId,
                    userQuestion = userMessage,
                    expertOpinions = expertOpinions,
                    provider = provider
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            emit(NetworkResult.Success(CommitteeResult.FinalSynthesis(result.data)))
                        }
                        is NetworkResult.Error -> {
                            emit(NetworkResult.Error("Ошибка при синтезе: ${result.message}"))
                        }
                        is NetworkResult.Loading -> {
                            // Промежуточное состояние
                        }
                    }
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error("Ошибка при синтезе финального ответа: ${e.message}"))
            }
        } else {
            emit(NetworkResult.Error("Не удалось получить ни одного мнения от экспертов"))
        }
    }
}

/**
 * Результат работы комитета экспертов
 */
public sealed class CommitteeResult {
    /**
     * Мнение одного эксперта
     */
    public data class ExpertOpinion(val opinion: ExpertOpinionResult) : CommitteeResult()

    /**
     * Финальный синтезированный ответ
     */
    public data class FinalSynthesis(val answer: String) : CommitteeResult()
}

/**
 * Мнение эксперта
 */
public data class ExpertOpinionResult(
    val expert: Expert,
    val opinion: String
)
