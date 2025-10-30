package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.TokensViewModel

internal fun tokensKoinModule(): Module {
    return module {
        viewModel {
            TokensViewModel(
                repository = get(),
            )
        }

        scope(tokensScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val Tokens_SCOPE_ID = "Tokens_SCOPE_ID"

internal val tokensScopeQualifier
    get() = named(Tokens_SCOPE_ID)