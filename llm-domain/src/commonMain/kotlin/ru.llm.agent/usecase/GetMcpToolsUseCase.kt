package ru.llm.agent.usecase

import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.usecase.base.NoInputSuspendUseCase

/**
 * Use case для получения списка доступных MCP инструментов.
 * Возвращает информацию об инструментах для отображения в UI.
 */
public class GetMcpToolsUseCase(
    private val mcpRepository: McpRepository
) : NoInputSuspendUseCase<List<McpToolInfo>> {

    override suspend fun invoke(): List<McpToolInfo> {
        return mcpRepository.getToolsInfo()
    }
}
