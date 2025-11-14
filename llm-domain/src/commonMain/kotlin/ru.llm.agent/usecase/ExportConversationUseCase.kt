package ru.llm.agent.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.llm.agent.NetworkResult
import ru.llm.agent.error.DomainError
import ru.llm.agent.exporter.ConversationExporter
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.repository.ConversationRepository

/**
 * Use case для экспорта диалога в указанный формат
 */
public class ExportConversationUseCase(
    private val conversationRepository: ConversationRepository,
    private val exporters: Map<ExportFormat, ConversationExporter>
) {
    /**
     * Экспортировать диалог
     *
     * @param conversationId ID диалога
     * @param format Формат экспорта
     * @return Flow с результатом экспорта (строка с данными)
     */
    public operator fun invoke(
        conversationId: String,
        format: ExportFormat
    ): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Получаем экспортер для указанного формата
            val exporter = exporters[format]
                ?: return@flow emit(NetworkResult.Error(
                    DomainError.ConfigurationError(
                        parameter = "exporter",
                        message = "Экспортер для формата ${format.name} не найден"
                    )
                ))

            // Получаем все сообщения диалога
            conversationRepository.getMessages(conversationId).collect { messages ->
                if (messages.isEmpty()) {
                    emit(NetworkResult.Error(DomainError.UnknownError("Диалог пуст, нечего экспортировать")))
                    return@collect
                }

                // Экспортируем диалог
                val exportedData = exporter.export(conversationId, messages)
                emit(NetworkResult.Success(exportedData))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(DomainError.UnknownError("Ошибка экспорта: ${e.message ?: "Неизвестная ошибка"}")))
        }
    }
}