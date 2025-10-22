package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.DiffTwoModelsViewModel

internal fun diffTwoModelsKoinModule(): Module {
    return module {
        viewModel {
            DiffTwoModelsViewModel(
                sendMessageToYandexGptUseCase = get(),
                sendMessageProxy = get()
            )
        }

        scope(diffTwoModelsScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val DIFF_TWO_MODELS_SCOPE_ID = "DIFF_TWO_MODELS_SCOPE_ID"

internal val diffTwoModelsScopeQualifier
    get() = named(DIFF_TWO_MODELS_SCOPE_ID)