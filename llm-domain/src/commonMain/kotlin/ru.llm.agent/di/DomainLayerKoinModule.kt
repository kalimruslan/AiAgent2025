package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteChainTwoAgentsUseCase
import ru.llm.agent.usecase.context.GetLocalContextUseCase
import ru.llm.agent.usecase.old.ParseJsonFormatUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.context.RemoveLocalContextUseCase
import ru.llm.agent.usecase.old.SendMessageToYandexGptUseCase
import ru.llm.agent.usecase.context.SaveLocalContextUseCase
import ru.llm.agent.usecase.old.SendMessageToProxyUseCase

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

    single<GetLocalContextUseCase>{
        GetLocalContextUseCase(
            repository = get<LocalDbRepository>()
        )
    }

    single<RemoveLocalContextUseCase>{
        RemoveLocalContextUseCase(
            repository = get<LocalDbRepository>()
        )
    }

    single<SaveLocalContextUseCase>{
        SaveLocalContextUseCase(
            repository = get<LocalDbRepository>(),
            conversationRepository = get<ConversationRepository>()
        )
    }

    single<SendMessageToProxyUseCase> {
        SendMessageToProxyUseCase(
            repository = get<LlmRepository>()
        )
    }

    single<ExecuteChainTwoAgentsUseCase> {
        ExecuteChainTwoAgentsUseCase(
            llmRepository = get<LlmRepository>()
        )
    }

}