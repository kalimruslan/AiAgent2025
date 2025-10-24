package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.InteractionTwoAgentsViewModel

internal fun interactionTwoAgentsKoinModule(): Module {
    return module {
        viewModel {
            InteractionTwoAgentsViewModel(
                executeChainTwoAgentsUseCase = get()
            )
        }

        scope(interactionTwoAgentsScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val INTERACTION_TWO_AGENTS_SCOPE_ID = "INTERACTION_TWO_AGENTS_SCOPE_ID"

internal val interactionTwoAgentsScopeQualifier
    get() = named(INTERACTION_TWO_AGENTS_SCOPE_ID)