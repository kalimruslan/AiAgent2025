package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.usecase.SendMessageToProxyUseCase
import ru.llm.agent.usecase.SendMessageToYandexGpt

public val useCasesModule: Module = module {
    single<SendMessageToProxyUseCase> {
        SendMessageToProxyUseCase(
            repository = get<LlmRepository>()
        )
    }

    single<SendMessageToYandexGpt> {
        SendMessageToYandexGpt(
            repository = get<LlmRepository>()
        )
    }
}