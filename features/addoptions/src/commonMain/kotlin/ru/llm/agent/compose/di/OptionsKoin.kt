package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.OptionsViewModel
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.GetOptionsFromDbUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SendOptionsToLocalDbUseCase

internal fun optionsKoinModule(): Module {
    return module {
        viewModel {
            OptionsViewModel(
                getOptionsFromDbUseCase = get<GetOptionsFromDbUseCase>(),
                sendOptionsToLocalDbUseCase = get<SendOptionsToLocalDbUseCase>()
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