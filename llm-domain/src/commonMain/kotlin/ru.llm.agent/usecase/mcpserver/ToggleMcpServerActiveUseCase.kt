package ru.llm.agent.usecase.mcpserver

import ru.llm.agent.repository.McpServerRepository
import ru.llm.agent.usecase.base.SuspendUseCase

/**
 * Параметры для переключения активности сервера
 */
public data class ToggleServerActiveParams(
    val serverId: Long,
    val isActive: Boolean
)

/**
 * Use case для переключения активности MCP сервера
 */
public class ToggleMcpServerActiveUseCase(
    private val repository: McpServerRepository
) : SuspendUseCase<ToggleServerActiveParams, Unit> {
    override suspend fun invoke(input: ToggleServerActiveParams) {
        repository.setServerActive(input.serverId, input.isActive)
    }
}