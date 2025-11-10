package ru.llm.agent.presentation.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.presentation.viewmodel.OptionsViewModel
import ru.llm.agent.usecase.context.GetLocalContextUseCase
import ru.llm.agent.usecase.context.RemoveLocalContextUseCase
import ru.llm.agent.usecase.context.SaveLocalContextUseCase

internal fun optionsKoinModule(): Module {
    return module {
        viewModel {
            OptionsViewModel(
                getLocalContextUseCase = get<GetLocalContextUseCase>(),
                saveLocalContextUseCase = get<SaveLocalContextUseCase>(),
                removeLocalContextUseCase = get<RemoveLocalContextUseCase>()
            )
        }
    }
}

// Константы для scope ID используются в UI слое
internal const val OPTIONS_SCOPE_ID = "OPTIONS_SCOPE_ID"

internal val optionsScopeQualifier
    get() = named(OPTIONS_SCOPE_ID)