package ru.llm.agent.mcp.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel

/**
 * Koin модуль для MCP feature
 */
val mcpModule = module {
    // ViewModel - ВАЖНО: single() чтобы был единственный экземпляр
    // Иначе ConversationViewModel и UI будут использовать разные экземпляры
    single { McpViewModel(getMcpToolsUseCase = get()) }
}