package ru.llm.agent.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.database.DatabaseDriverFactory
import ru.llm.agent.sdk.BuildConfig

public actual val yandexDeveloperToken: String
    get() = BuildConfig.YANDEX_API_KEY

public actual val proxyApiToken: String
    get() = BuildConfig.PROXY_API_KEY
