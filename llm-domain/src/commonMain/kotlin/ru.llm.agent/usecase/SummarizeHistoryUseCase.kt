package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository

/**
 * Конфигурация для суммаризации истории диалога
 *
 * @param tokenThreshold Порог использования токенов для запуска суммаризации (0.0 - 1.0)
 * @param keepLastMessages Сколько последних сообщений НЕ суммаризировать
 * @param summarizationMaxTokens Максимальное количество токенов для суммаризации
 * @param minMessagesToSummarize Минимальное количество сообщений для суммаризации
 */
public data class SummarizationConfig(
    val tokenThreshold: Double = 0.75,
    val keepLastMessages: Int = 3,
    val summarizationMaxTokens: Int = 300,
    val minMessagesToSummarize: Int = 5
)

/**
 * Use case для суммаризации истории диалога при приближении к лимиту токенов.
 *
 * Логика работы:
 * 1. Проверяет, превышен ли порог использования токенов
 * 2. Если да - берет старые сообщения (кроме последних N)
 * 3. Объединяет их в один текст и суммаризирует через YandexGPT
 * 4. Сохраняет суммаризацию как SYSTEM сообщение
 * 5. Удаляет старые сообщения из БД
 *
 * @param conversationRepository Репозиторий для работы с сообщениями
 * @param llmRepository Репозиторий для вызова LLM API (суммаризация)
 * @param config Конфигурация суммаризации
 */
public class SummarizeHistoryUseCase(
    private val conversationRepository: ConversationRepository,
    private val llmRepository: LlmRepository,
    private val config: SummarizationConfig = SummarizationConfig()
) {
    /**
     * Проверить и выполнить суммаризацию если необходимо
     *
     * @param conversationId ID диалога
     * @param currentTokens Текущее количество использованных токенов
     * @param maxTokens Максимальное количество токенов
     * @param provider Провайдер LLM для суммаризации
     * @return Flow с результатом (true если суммаризация была выполнена, false если не требовалась)
     */
    public suspend operator fun invoke(
        conversationId: String,
        currentTokens: Int,
        maxTokens: Int,
        provider: LlmProvider
    ): Flow<NetworkResult<Boolean>> = flow {
        emit(NetworkResult.Loading())

        try {
            val usageRatio = currentTokens.toDouble() / maxTokens.toDouble()
            if (usageRatio < config.tokenThreshold) {
                emit(NetworkResult.Success(false))
                return@flow
            }

            val allMessages = conversationRepository.getMessagesByConversationSync(conversationId)

            val eligibleMessages = allMessages.filter { message ->
                (message.role == Role.USER || message.role == Role.ASSISTANT) &&
                !message.isSummarized
            }

            if (eligibleMessages.size < config.minMessagesToSummarize) {
                emit(NetworkResult.Success(false))
                return@flow
            }

            val messagesToKeep = eligibleMessages.takeLast(config.keepLastMessages)
            val messagesToSummarize = eligibleMessages.dropLast(config.keepLastMessages)

            if (messagesToSummarize.isEmpty()) {
                emit(NetworkResult.Success(false))
                return@flow
            }

            val textToSummarize = buildTextForSummarization(messagesToSummarize)

            val savedTokens = messagesToSummarize.sumOf { it.totalTokens ?: 0 }

            val summarizedText = llmRepository.summarizeYandexGPTText(
                text = textToSummarize,
                model = provider.modelId,
                maxTokens = config.summarizationMaxTokens
            )

            if (summarizedText.isEmpty()) {
                emit(NetworkResult.Error(
                    DomainError.BusinessLogicError(
                        reason = "EMPTY_SUMMARIZATION_RESPONSE",
                        message = "Не удалось суммаризировать историю: пустой ответ от LLM"
                    )
                ))
                return@flow
            }

            conversationRepository.saveSystemMessage(
                conversationId = conversationId,
                text = "📝 [Краткое содержание предыдущих ${messagesToSummarize.size} сообщений]\n\n$summarizedText",
                isSummarized = true,
                totalTokens = config.summarizationMaxTokens
            )

            // Удаляем старые сообщения
            val messageIdsToDelete = messagesToSummarize.map { it.id }
            conversationRepository.deleteMessages(messageIdsToDelete)

            emit(NetworkResult.Success(true))

        } catch (e: Exception) {
            emit(NetworkResult.Error(
                DomainError.UnknownError(
                    message = "Ошибка при суммаризации истории: ${e.message}",
                    exception = e
                )
            ))
        }
    }

    /**
     * Формирует текст для суммаризации из списка сообщений
     */
    private fun buildTextForSummarization(messages: List<ru.llm.agent.model.conversation.ConversationMessage>): String {
        return messages.joinToString(separator = "\n\n") { message ->
            val roleName = when (message.role) {
                Role.USER -> "Пользователь"
                Role.ASSISTANT -> "Ассистент"
                Role.SYSTEM -> "Система"
                Role.FUNCTION -> "Функция"
                Role.NONE -> "Нет роли"
            }
            "$roleName: ${message.text}"
        }
    }
}