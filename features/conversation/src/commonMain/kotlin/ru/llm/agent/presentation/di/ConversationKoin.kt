package ru.llm.agent.presentation.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.presentation.viewmodel.ConversationViewModel
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.ExportConversationUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.GetMessageTokenCountUseCase
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.GetSummarizationInfoUseCase
import ru.llm.agent.usecase.GetTokenUsageUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SummarizeHistoryUseCase

internal fun conversationKoinModule(): Module {
    return module {
        viewModel {
            ConversationViewModel(
                conversationUseCase = get<ConversationUseCase>(),
                sendConversationMessageUseCase = get<SendConversationMessageUseCase>(),
                getSelectedProviderUseCase = get<GetSelectedProviderUseCase>(),
                saveSelectedProviderUseCase = get<SaveSelectedProviderUseCase>(),
                getMessagesWithExpertOpinionsUseCase = get<GetMessagesWithExpertOpinionsUseCase>(),
                executeCommitteeUseCase = get<ExecuteCommitteeUseCase>(),
                getTokenUsageUseCase = get<GetTokenUsageUseCase>(),
                getMessageTokenCountUseCase = get<GetMessageTokenCountUseCase>(),
                summarizeHistoryUseCase = get<SummarizeHistoryUseCase>(),
                getSummarizationInfoUseCase = get<GetSummarizationInfoUseCase>(),
                exportConversationUseCase = get<ExportConversationUseCase>()
            )
        }
    }
}

// Константы для scope ID используются в UI слое
internal const val CONVERSATION_CHAT_SCOPE_ID = "CONVERSATION_CHAT_SCOPE_ID"

internal val conversationChatScopeQualifier
    get() = named(CONVERSATION_CHAT_SCOPE_ID)
