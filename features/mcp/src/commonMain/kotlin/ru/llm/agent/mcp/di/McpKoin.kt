package ru.llm.agent.mcp.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel

/**
 * Koin модуль для MCP feature
 */
val mcpModule = module {
    // ViewModel
    viewModelOf(::McpViewModel)
}