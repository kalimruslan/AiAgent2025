package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.usecase.ParseJsonFormatUseCase
import ru.llm.agent.usecase.SendMessageToYandexGptUseCase

public val useCasesModule: Module = module {
    single<SendMessageToYandexGptUseCase> {
        SendMessageToYandexGptUseCase(
            repository = get<LlmRepository>()
        )
    }
    single<ParseJsonFormatUseCase> {
        ParseJsonFormatUseCase()
    }
}