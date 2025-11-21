package ru.llm.agent.usecase.mcpserver

import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.repository.McpServerRepository
import ru.llm.agent.usecase.base.SuspendUseCase

/**
 * Use case для добавления нового MCP сервера
 */
public class AddMcpServerUseCase(
    private val repository: McpServerRepository
) : SuspendUseCase<McpServer, Long> {
    override suspend fun invoke(input: McpServer): Long {
        return repository.addServer(input)
    }
}