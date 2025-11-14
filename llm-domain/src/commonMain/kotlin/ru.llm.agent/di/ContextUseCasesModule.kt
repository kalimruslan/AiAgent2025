package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.context.GetLocalContextUseCase
import ru.llm.agent.usecase.context.RemoveLocalContextUseCase
import ru.llm.agent.usecase.context.SaveLocalContextUseCase

/**
 * Модуль Koin для Use Cases, связанных с контекстом и настройками
 */
public val contextUseCasesModule: Module = module {
    // Управление локальным контекстом
    single<GetLocalContextUseCase> {
        GetLocalContextUseCase(
            repository = get<LocalDbRepository>()
        )
    }

    single<RemoveLocalContextUseCase> {
        RemoveLocalContextUseCase(
            repository = get<LocalDbRepository>()
        )
    }

    single<SaveLocalContextUseCase> {
        SaveLocalContextUseCase(
            repository = get<LocalDbRepository>(),
            conversationRepository = get<ConversationRepository>()
        )
    }

    // Управление настройками провайдера
    single<GetSelectedProviderUseCase> {
        GetSelectedProviderUseCase(
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }

    single<SaveSelectedProviderUseCase> {
        SaveSelectedProviderUseCase(
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }
}