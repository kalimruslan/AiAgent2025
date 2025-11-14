package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.MessageRepository
import ru.llm.agent.repository.SummarizationRepository
import ru.llm.agent.repository.TokenManagementRepository
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.GetConversationStateUseCase
import ru.llm.agent.usecase.GetMessageTokenCountUseCase
import ru.llm.agent.usecase.GetSummarizationInfoUseCase
import ru.llm.agent.usecase.GetTokenUsageUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SummarizeHistoryUseCase

/**
 * Модуль Koin для Use Cases, связанных с диалогами
 */
public val conversationUseCasesModule: Module = module {
    // Основные операции с диалогами
    single<ConversationUseCase> {
        ConversationUseCase(
            repository = get<ConversationRepository>()
        )
    }

    single<SendConversationMessageUseCase> {
        SendConversationMessageUseCase(
            conversationRepository = get<ConversationRepository>(),
            messageSendingService = get<MessageSendingService>()
        )
    }

    // Управление токенами
    single<GetTokenUsageUseCase> {
        GetTokenUsageUseCase(
            conversationRepository = get<ConversationRepository>()
        )
    }

    single<GetMessageTokenCountUseCase> {
        GetMessageTokenCountUseCase(
            llmRepository = get<LlmRepository>(),
            conversationRepository = get<ConversationRepository>()
        )
    }

    // Суммаризация истории
    single<SummarizeHistoryUseCase> {
        SummarizeHistoryUseCase(
            conversationRepository = get<ConversationRepository>(),
            llmRepository = get<LlmRepository>()
        )
    }

    single<GetSummarizationInfoUseCase> {
        GetSummarizationInfoUseCase(
            summarizationRepository = get<SummarizationRepository>()
        )
    }

    // Новые Use Cases после рефакторинга
    single<GetConversationStateUseCase> {
        GetConversationStateUseCase(
            messageRepository = get<MessageRepository>(),
            tokenManagementRepository = get<TokenManagementRepository>(),
            summarizationRepository = get<SummarizationRepository>()
        )
    }
}