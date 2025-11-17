package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.InteractYaGptWithMcpService
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.error.ErrorLogger
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.usecase.ExecuteChainTwoAgentsUseCase
import ru.llm.agent.usecase.GetMcpToolsUseCase
import ru.llm.agent.usecase.ParseAssistantResponseUseCase
import ru.llm.agent.usecase.SystemPromptBuilder

/**
 * Модуль Koin для базовых утилитарных Use Cases
 */
public val coreUseCasesModule: Module = module {
    // Утилитарные компоненты для парсинга и генерации промптов
    single<ParseAssistantResponseUseCase> {
        ParseAssistantResponseUseCase()
    }

    single<SystemPromptBuilder> {
        SystemPromptBuilder()
    }

    // Централизованный ErrorLogger для обработки всех ошибок
    single<ErrorLogger> {
        ErrorLogger(
            logger = createLogger("ErrorLogger")
        )
    }

    // Мультиагентные сценарии
    single<ExecuteChainTwoAgentsUseCase> {
        ExecuteChainTwoAgentsUseCase(
            llmRepository = get<LlmRepository>(),
            parseAssistantResponseUseCase = get<ParseAssistantResponseUseCase>(),
            systemPromptBuilder = get<SystemPromptBuilder>()
        )
    }

    // Интеграция с MCP (Model Context Protocol)
    single<InteractYaGptWithMcpService> {
        InteractYaGptWithMcpService(
            llmRepository = get<LlmRepository>(),
            mcpRepository = get<McpRepository>(),
            logger = createLogger("McpService")
        )
    }

    // Use case для получения списка MCP инструментов
    single<GetMcpToolsUseCase> {
        GetMcpToolsUseCase(
            mcpRepository = get<McpRepository>()
        )
    }
}