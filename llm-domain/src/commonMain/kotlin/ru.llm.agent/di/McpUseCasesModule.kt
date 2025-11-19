package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.usecase.ChatWithMcpToolsUseCase
import ru.llm.agent.usecase.GetMcpToolsUseCase
import ru.llm.agent.usecase.MonitorBoardSummaryUseCase

/**
 * Модуль Koin для MCP Use Cases
 */
public val mcpUseCasesModule: Module = module {
    // Use case для получения списка инструментов
    single<GetMcpToolsUseCase> {
        GetMcpToolsUseCase(
            mcpRepository = get<McpRepository>()
        )
    }

    // Use case для чата с поддержкой MCP tool calling
    single<ChatWithMcpToolsUseCase> {
        ChatWithMcpToolsUseCase(
            conversationRepository = get<ConversationRepository>(),
            llmRepository = get<ru.llm.agent.repository.LlmRepository>(),
            mcpRepository = get<McpRepository>(),
            logger = createLogger("ChatWithMcpToolsUseCase")
        )
    }

    // Use case для периодического мониторинга Trello доски
    single<MonitorBoardSummaryUseCase> {
        MonitorBoardSummaryUseCase(
            mcpRepository = get<McpRepository>(),
            logger = createLogger("MonitorBoardSummaryUseCase")
        )
    }
}
