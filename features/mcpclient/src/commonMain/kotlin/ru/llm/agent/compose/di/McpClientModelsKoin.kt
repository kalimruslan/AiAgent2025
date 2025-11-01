package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.McpViewModel

internal fun mcpClientKoinModule(): Module {
    return module {
        viewModel {
            McpViewModel()
        }

        scope(mcpClientScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val MCP_CLIENT_SCOPE_ID = "DMCP_CLIENT_SCOPE_ID"

internal val mcpClientScopeQualifier
    get() = named(MCP_CLIENT_SCOPE_ID)