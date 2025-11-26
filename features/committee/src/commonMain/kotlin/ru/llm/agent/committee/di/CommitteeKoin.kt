package ru.llm.agent.committee.di

import org.koin.dsl.module
import ru.llm.agent.committee.presentation.viewmodel.CommitteeViewModel

/**
 * Koin модуль для Committee feature
 */
val committeeModule = module {
    // ViewModel - ВАЖНО: single() чтобы был единственный экземпляр
    // Иначе ConversationViewModel и UI будут использовать разные экземпляры
    single { CommitteeViewModel() }
}