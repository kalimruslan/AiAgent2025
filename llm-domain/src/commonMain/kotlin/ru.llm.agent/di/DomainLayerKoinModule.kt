package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ParseJsonFormatUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
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

    single<ConversationUseCase>{
        ConversationUseCase(
            repository = get<ConversationRepository>()
        )
    }

    single<SendConversationMessageUseCase>{
        SendConversationMessageUseCase(
            repository = get<ConversationRepository>()
        )
    }
}