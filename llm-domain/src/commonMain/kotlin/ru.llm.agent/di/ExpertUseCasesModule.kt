package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ExpertRepository
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.SendMessageWithCustomPromptUseCase
import ru.llm.agent.usecase.SynthesizeExpertOpinionsUseCase
import ru.llm.agent.usecase.SystemPromptBuilder

/**
 * Модуль Koin для Use Cases, связанных с экспертами (Committee режим)
 */
public val expertUseCasesModule: Module = module {
    // Работа с кастомными промптами
    single<SendMessageWithCustomPromptUseCase> {
        SendMessageWithCustomPromptUseCase(
            messageSendingService = get<MessageSendingService>(),
            providerConfigRepository = get<ProviderConfigRepository>()
        )
    }

    // Синтез мнений экспертов
    single<SynthesizeExpertOpinionsUseCase> {
        SynthesizeExpertOpinionsUseCase(
            sendMessageWithCustomPromptUseCase = get<SendMessageWithCustomPromptUseCase>(),
            expertRepository = get<ExpertRepository>(),
            systemPromptBuilder = get<SystemPromptBuilder>(),
            logger = createLogger("Synthesis")
        )
    }

    // Получение сообщений с мнениями экспертов
    single<GetMessagesWithExpertOpinionsUseCase> {
        GetMessagesWithExpertOpinionsUseCase(
            conversationRepository = get<ConversationRepository>()
        )
    }

    // Выполнение Committee режима
    single<ExecuteCommitteeUseCase> {
        ExecuteCommitteeUseCase(
            conversationRepository = get<ConversationRepository>(),
            expertRepository = get<ExpertRepository>(),
            sendMessageWithCustomPromptUseCase = get<SendMessageWithCustomPromptUseCase>(),
            synthesizeExpertOpinionsUseCase = get<SynthesizeExpertOpinionsUseCase>(),
            logger = createLogger("Committee")
        )
    }
}