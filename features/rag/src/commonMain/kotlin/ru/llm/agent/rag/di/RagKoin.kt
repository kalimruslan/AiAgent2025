package ru.llm.agent.rag.di

import org.koin.dsl.module
import ru.llm.agent.rag.presentation.viewmodel.RagViewModel

/**
 * Koin модуль для RAG feature
 */
val ragFeatureModule = module {
    // ViewModel - ВАЖНО: single() чтобы был единственный экземпляр
    // Иначе ConversationViewModel и UI будут использовать разные экземпляры
    single {
        RagViewModel(
            indexTextUseCase = get(),
            getRagIndexStatsUseCase = get(),
            clearRagIndexUseCase = get()
        )
    }
}
