package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.usecase.mcpserver.AddMcpServerUseCase
import ru.llm.agent.usecase.mcpserver.DeleteMcpServerUseCase
import ru.llm.agent.usecase.mcpserver.GetAllMcpServersUseCase
import ru.llm.agent.usecase.mcpserver.ToggleMcpServerActiveUseCase

/**
 * Koin модуль для use cases управления MCP серверами
 */
public val mcpServerUseCasesModule: Module = module {
    factory { GetAllMcpServersUseCase(get()) }
    factory { AddMcpServerUseCase(get()) }
    factory { DeleteMcpServerUseCase(get()) }
    factory { ToggleMcpServerActiveUseCase(get()) }
}