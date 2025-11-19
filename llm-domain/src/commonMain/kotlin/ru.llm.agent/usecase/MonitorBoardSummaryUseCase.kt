package ru.llm.agent.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.repository.McpRepository

/**
 * Use case для периодического мониторинга статистики Trello доски.
 *
 * Запускает фоновый процесс, который каждые N минут:
 * 1. Вызывает MCP инструмент для получения саммари доски
 * 2. Возвращает саммари через Flow
 * 3. ViewModel получает саммари и отправляет в чат с агентом
 *
 * @param mcpRepository Репозиторий для работы с MCP инструментами
 * @param logger Логгер для отладки
 */
public class MonitorBoardSummaryUseCase(
    private val mcpRepository: McpRepository,
    private val logger: Logger,
) {
    /**
     * Запускает мониторинг доски Trello
     *
     * @param boardId ID доски Trello для мониторинга
     * @param intervalMinutes Интервал проверки в минутах (по умолчанию 5)
     * @return Flow с саммари доски, эмитится каждые intervalMinutes минут
     */
    public operator fun invoke(
        boardId: String,
        intervalMinutes: Int = 5,
    ): Flow<String> = flow {
        while (true) {
            try {
                logger.info("Получение саммари доски $boardId")

                // Вызываем MCP инструмент для получения саммари
                val summary = mcpRepository.callTool(
                    name = "trello_getSummary",
                    arguments = buildJsonObject {
                        put("boardId", boardId)
                    }
                )

                logger.info("Саммари получен: $summary")

                // Эмитим результат
                emit(summary)

                // Ждём перед следующей проверкой
                val delayMs = intervalMinutes * 60 * 1000L
                logger.info("Ожидание $intervalMinutes минут до следующей проверки")
                delay(delayMs)

            } catch (e: Exception) {
                logger.info("Ошибка при получении саммари: ${e.message}")
                // Продолжаем работу даже при ошибке, через intervalMinutes повторим попытку
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }
}