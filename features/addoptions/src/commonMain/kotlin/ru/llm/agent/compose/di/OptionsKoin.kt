package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.OptionsViewModel
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

        scope(optionsScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val OPTIONS_SCOPE_ID = "OPTIONS_SCOPE_ID"

internal val optionsScopeQualifier
    get() = named(OPTIONS_SCOPE_ID)