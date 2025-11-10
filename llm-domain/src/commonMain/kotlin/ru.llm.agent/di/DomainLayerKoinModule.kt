package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.InteractYaGptWithMcpService
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ExpertRepository
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteChainTwoAgentsUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.ParseAssistantResponseUseCase
import ru.llm.agent.usecase.SystemPromptBuilder
import ru.llm.agent.usecase.context.GetLocalContextUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.context.RemoveLocalContextUseCase
import ru.llm.agent.usecase.context.SaveLocalContextUseCase

public val useCasesModule: Module = module {
    // Утилитарные компоненты для парсинга и генерации промптов
    single<ParseAssistantResponseUseCase> {
        ParseAssistantResponseUseCase()
    }

    single<SystemPromptBuilder> {
        SystemPromptBuilder()
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

    single<ExecuteChainTwoAgentsUseCase> {
        ExecuteChainTwoAgentsUseCase(
            llmRepository = get<LlmRepository>()
        )
    }

    single<InteractYaGptWithMcpService>{
        InteractYaGptWithMcpService(
            llmRepository = get<LlmRepository>(),
            mcpRepository = get<McpRepository>(),
            logger = createLogger("McpService")
        )
    }

    single<ExecuteCommitteeUseCase>{
        ExecuteCommitteeUseCase(
            conversationRepository = get<ConversationRepository>(),
            expertRepository = get<ExpertRepository>(),
            logger = createLogger("Committee")
        )
    }

}