package ru.llm.agent.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.database.DatabaseDriverFactory


public actual val yandexDeveloperToken: String
    get() = System.getProperty("YANDEX_API_KEY")?: error("YANDEX_API_KEY not found")
