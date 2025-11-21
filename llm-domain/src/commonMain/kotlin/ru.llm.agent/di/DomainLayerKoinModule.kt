package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Главный модуль Domain слоя.
 * Объединяет все подмодули Use Cases для упрощения импорта.
 */
public val domainKoinModule: Module = module {
    includes(
        coreUseCasesModule,
        conversationUseCasesModule,
        expertUseCasesModule,
        contextUseCasesModule,
        mcpUseCasesModule,
        mcpServerUseCasesModule
    )
}