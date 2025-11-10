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
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteChainTwoAgentsUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.ParseAssistantResponseUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.SendMessageWithCustomPromptUseCase
import ru.llm.agent.usecase.SynthesizeExpertOpinionsUseCase
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
            conversationRepository = get<ConversationRepository>(),
            messageSendingService = get<MessageSendingService>()
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
            llmRepository = get<LlmRepository>(),
            parseAssistantResponseUseCase = get<ParseAssistantResponseUseCase>(),
            systemPromptBuilder = get<SystemPromptBuilder>()
        )
    }

    single<InteractYaGptWithMcpService>{
        InteractYaGptWithMcpService(
            llmRepository = get<LlmRepository>(),
            mcpRepository = get<McpRepository>(),
            logger = createLogger("McpService")
        )
    }

    single<SendMessageWithCustomPromptUseCase>{
        SendMessageWithCustomPromptUseCase(
            messageSendingService = get<MessageSendingService>(),
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }

    single<GetSelectedProviderUseCase>{
        GetSelectedProviderUseCase(
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }

    single<SaveSelectedProviderUseCase>{
        SaveSelectedProviderUseCase(
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }

    single<SynthesizeExpertOpinionsUseCase>{
        SynthesizeExpertOpinionsUseCase(
            sendMessageWithCustomPromptUseCase = get<SendMessageWithCustomPromptUseCase>(),
            expertRepository = get<ExpertRepository>(),
            systemPromptBuilder = get<SystemPromptBuilder>(),
            logger = createLogger("Synthesis")
        )
    }

    single<GetMessagesWithExpertOpinionsUseCase>{
        GetMessagesWithExpertOpinionsUseCase(
            conversationRepository = get<ConversationRepository>()
        )
    }

    single<ExecuteCommitteeUseCase>{
        ExecuteCommitteeUseCase(
            conversationRepository = get<ConversationRepository>(),
            expertRepository = get<ExpertRepository>(),
            sendMessageWithCustomPromptUseCase = get<SendMessageWithCustomPromptUseCase>(),
            synthesizeExpertOpinionsUseCase = get<SynthesizeExpertOpinionsUseCase>(),
            logger = createLogger("Committee")
        )
    }

}