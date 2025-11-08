package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ExpertRepository
import java.util.logging.Logger
import kotlin.math.exp

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
    private val expertRepository: ExpertRepository
) {

    /**
     * Выполнить обсуждение в комитете экспертов
     *
     * @param conversationId ID диалога
     * @param userMessage Сообщение пользователя
     * @param experts Список выбранных экспертов
     * @param provider LLM провайдер для общения с экспертами
     * @param messageId ID сообщения пользователя (для связи мнений)
     *
     * @return Flow с результатами: сначала мнения экспертов, затем финальный синтез
     */
    public suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        experts: List<Expert>,
        provider: LlmProvider,
        messageId: Long
    ): Flow<NetworkResult<CommitteeResult>> = flow {
        emit(NetworkResult.Loading())

        Logger.getLogger("Committe").info("Execute comitte START - experts - ${experts.map { it.name }}")

        if (experts.isEmpty()) {
            emit(NetworkResult.Error("Не выбраны эксперты"))
            return@flow
        }

        val expertOpinions = mutableListOf<ExpertOpinionResult>()

        // 1. Собираем мнения от каждого эксперта
        for (expert in experts) {
            emit(NetworkResult.Loading())

            try {
                // Создаем временный диалог для эксперта с его системным промптом
                val expertConversationId = "$conversationId-expert-${expert.id}"

                // Инициализируем диалог для эксперта
                //conversationRepository.initializeConversation(expertConversationId)
                Logger.getLogger("Committe").info("Send message to expert - ${expert.name}, conversationId - $conversationId,\n" +
                        "message - ${expert.systemPrompt}\n\nВопрос: $userMessage")
                // Получаем мнение эксперта
                var expertOpinion = ""
                conversationRepository.sendMessage(
                    conversationId = expertConversationId,
                    message = "${expert.systemPrompt}\n\nВопрос: $userMessage",
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
                                messageId = messageId,
                                conversationId = conversationId,
                                opinion = expertOpinion,
                                timestamp = System.currentTimeMillis(),
                                originalResponse = result.data.text
                            )

                            val opinionResult = ExpertOpinionResult(
                                expert = expert,
                                opinion = expertOpinion
                            )
                            Logger.getLogger("Committe").info("Expert ${expert.name} opinion result - $opinionResult")
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

            val synthesisPrompt = buildSynthesisPrompt(userMessage, expertOpinions)

            try {
                // Создаем временный диалог для синтеза
                val synthesisConversationId = "$conversationId-synthesis"

                val synthesisSystemPrompt = """
                    Ты - координатор комитета экспертов. Твоя задача - синтезировать финальный ответ
                    на основе мнений всех экспертов. Учитывай все точки зрения, находи общие моменты
                    и различия. Создай структурированный и полный ответ.

                    Отвечай строго в JSON формате:
                    {
                      "answer": "твой синтезированный ответ",
                      "is_continue": false,
                      "is_complete": true
                    }
                """.trimIndent()

                Logger.getLogger("Committe").info("Synthessys prompt - $synthesisSystemPrompt\n\n$synthesisPrompt")

                var finalAnswer = ""
                conversationRepository.sendMessage(
                    conversationId = synthesisConversationId,
                    message = "$synthesisSystemPrompt\n\n$synthesisPrompt",
                    provider = provider
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            finalAnswer = result.data.text
                            Logger.getLogger("Committe").info("Synthessys SUCCESS - $finalAnswer")
                            emit(NetworkResult.Success(CommitteeResult.FinalSynthesis(finalAnswer)))
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

    /**
     * Построить промпт для синтеза финального ответа
     */
    private fun buildSynthesisPrompt(
        userMessage: String,
        expertOpinions: List<ExpertOpinionResult>
    ): String {
        val opinionsText = expertOpinions.joinToString("\n\n") { result ->
            "${result.expert.icon} **${result.expert.name}**:\n${result.opinion}"
        }

        return """
            Вопрос пользователя: "$userMessage"

            Мнения экспертов:
            $opinionsText

            Задача: Проанализируй все мнения экспертов и создай финальный, структурированный ответ.
            Учти все важные моменты, которые упомянули эксперты. Если есть противоречия - укажи их.
            Если эксперты согласны - выдели общую позицию.
        """.trimIndent()
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
