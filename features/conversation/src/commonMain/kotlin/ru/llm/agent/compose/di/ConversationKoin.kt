package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.ConversationViewModel
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase

internal fun conversationKoinModule(): Module {
    return module {
        viewModel {
            ConversationViewModel(
                conversationUseCase = get<ConversationUseCase>(),
                sendConversationMessageUseCase = get<SendConversationMessageUseCase>(),
                conversationRepository = get(),
                executeCommitteeUseCase = get<ExecuteCommitteeUseCase>()
            )
        }

        scope(conversationChatScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val CONVERSATION_CHAT_SCOPE_ID = "CONVERSATION_CHAT_SCOPE_ID"

internal val conversationChatScopeQualifier
    get() = named(CONVERSATION_CHAT_SCOPE_ID)