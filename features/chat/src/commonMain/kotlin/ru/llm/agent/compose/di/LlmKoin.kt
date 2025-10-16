package ru.llm.agent.compose.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.compose.presenter.ChatLlmViewModel
import ru.llm.agent.usecase.SendMessageToYandexGpt

internal fun llmKoinModule(): Module {
    return module {
        viewModel {
            ChatLlmViewModel(
                sendMessageToYaGPT = get<SendMessageToYandexGpt>()
            )
        }

        scope(llmChatScopeQualifier) {
            // Сюда можно добавлять зависимости  у которых время жизни ограничен этим скоупом
        }
    }
}

internal const val LLM_CHAT_SCOPE_ID = "LLM_CHAT_SCOPE_ID"

internal val llmChatScopeQualifier
    get() = named(LLM_CHAT_SCOPE_ID)