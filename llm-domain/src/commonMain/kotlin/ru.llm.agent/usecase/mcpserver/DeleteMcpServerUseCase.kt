package ru.llm.agent.usecase.mcpserver

import ru.llm.agent.repository.McpServerRepository
import ru.llm.agent.usecase.base.SuspendUseCase

/**
 * Use case для удаления MCP сервера
 */
public class DeleteMcpServerUseCase(
    private val repository: McpServerRepository
) : SuspendUseCase<Long, Unit> {
    override suspend fun invoke(input: Long) {
        repository.deleteServer(input)
    }
}