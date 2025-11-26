package ru.llm.agent.usecase.rag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.RagRepository
import ru.llm.agent.service.MessageSendingService
import java.util.logging.Logger

/**
 * Use case для отправки сообщения с использованием RAG контекста
 * Находит релевантные документы и добавляет их в контекст сообщения
 *
 * ВАЖНО: Сохраняет в БД оригинальный вопрос пользователя (без RAG контекста),
 * но отправляет в LLM расширенное сообщение с контекстом
 */
public class AskWithRagUseCase(
    private val ragRepository: RagRepository,
    private val conversationRepository: ConversationRepository,
    private val messageSendingService: MessageSendingService
) {
    public suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        provider: LlmProvider,
        topK: Int = 3,
        threshold: Double = 0.3,
        useMmr: Boolean = true,
        mmrLambda: Double = 0.5,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Flow<NetworkResult<ConversationMessage>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Валидация
            if (userMessage.isBlank()) {
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

            // 1. Сохраняем ОРИГИНАЛЬНОЕ сообщение пользователя (без RAG контекста)
            conversationRepository.saveUserMessage(conversationId, userMessage, provider)

            // 2. Ищем релевантные документы
            val relevantDocs = ragRepository.search(
                query = userMessage,
                topK = topK,
                threshold = threshold,
                useMmr = useMmr,
                mmrLambda = mmrLambda
            )

            // 3. Формируем дополнительный контекст из найденных документов
            val ragContext = if (relevantDocs.isNotEmpty()) {
                buildString {
                    appendLine("Контекст из базы знаний:")
                    appendLine()
                    relevantDocs.forEachIndexed { index, doc ->
                        appendLine("Документ ${index + 1} (схожесть: ${String.format("%.2f", doc.similarity)}):")
                        appendLine(doc.text)
                        appendLine()
                    }
                    appendLine("---")
                    appendLine("Используй информацию из контекста выше для ответа на вопрос пользователя.")
                    appendLine()
                }
            } else {
                ""
            }

            Logger.getLogger("RAG").info(
                ragContext
            )

            // 4. Получаем всю историю диалога
            val allMessages = conversationRepository.getMessagesByConversationSync(conversationId)

            // 5. Модифицируем последнее сообщение пользователя, добавляя RAG контекст
            val messagesWithRag = if (ragContext.isNotEmpty()) {
                allMessages.toMutableList().apply {
                    // Находим последнее USER сообщение и добавляем к нему контекст
                    val lastUserIndex = indexOfLast { it.role == ru.llm.agent.model.Role.USER }
                    if (lastUserIndex >= 0) {
                        val lastUserMsg = this[lastUserIndex]
                        this[lastUserIndex] = lastUserMsg.copy(
                            text = "$ragContext\nВопрос: ${lastUserMsg.text}"
                        )
                    }
                }
            } else {
                allMessages
            }

            messagesWithRag.forEach { Logger.getLogger("RAG").info(it.text) }

            // 6. Отправляем расширенное сообщение в LLM
            messageSendingService.sendMessages(
                conversationId = conversationId,
                messages = messagesWithRag,
                provider = provider,
                temperature = temperature,
                maxTokens = maxTokens
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Сохраняем ответ ассистента в БД
                        val assistantId = conversationRepository.saveAssistantMessage(
                            result.data.conversationMessage
                        )

                        // Возвращаем сообщение с правильным ID
                        emit(
                            NetworkResult.Success(
                                result.data.conversationMessage.copy(id = assistantId)
                            )
                        )
                    }
                    is NetworkResult.Error -> {
                        emit(NetworkResult.Error(result.error))
                    }
                    is NetworkResult.Loading -> {
                        emit(NetworkResult.Loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(
                NetworkResult.Error(
                    DomainError.UnknownError(
                        message = "Ошибка при отправке сообщения с RAG: ${e.message}"
                    )
                )
            )
        }
    }
}