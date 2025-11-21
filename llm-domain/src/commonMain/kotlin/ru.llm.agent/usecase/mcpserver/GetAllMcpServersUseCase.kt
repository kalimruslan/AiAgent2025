package ru.llm.agent.usecase.mcpserver

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.repository.McpServerRepository
import ru.llm.agent.usecase.base.FlowUseCase

/**
 * Use case для получения всех MCP серверов
 */
public class GetAllMcpServersUseCase(
    private val repository: McpServerRepository
) : FlowUseCase<Unit, List<McpServer>> {
    override fun invoke(input: Unit): Flow<List<McpServer>> {
        return repository.getAllServers()
    }
}