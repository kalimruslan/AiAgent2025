package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.usecase.rag.AskWithRagUseCase
import ru.llm.agent.usecase.rag.ClearRagIndexUseCase
import ru.llm.agent.usecase.rag.GetMessagesWithRagSourcesUseCase
import ru.llm.agent.usecase.rag.GetRagIndexStatsUseCase
import ru.llm.agent.usecase.rag.IndexTextUseCase
import ru.llm.agent.usecase.rag.SearchRagDocumentsUseCase

/**
 * Koin модуль для RAG use cases
 */
public val ragUseCasesModule: Module = module {
    // Индексация текста
    factory { IndexTextUseCase(ragRepository = get()) }

    // Поиск документов
    factory { SearchRagDocumentsUseCase(ragRepository = get()) }

    // Статистика индекса
    factory { GetRagIndexStatsUseCase(ragRepository = get()) }

    // Очистка индекса
    factory { ClearRagIndexUseCase(ragRepository = get()) }

    // Вопросы с RAG контекстом
    factory {
        AskWithRagUseCase(
            ragRepository = get(),
            conversationRepository = get(),
            messageSendingService = get(),
            ragSourceRepository = get()
        )
    }

    // Получение сообщений с источниками RAG
    factory {
        GetMessagesWithRagSourcesUseCase(
            conversationRepository = get(),
            ragSourceRepository = get()
        )
    }
}