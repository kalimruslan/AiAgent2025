package ru.llm.agent.di

import ru.llm.agent.sdk.BuildConfig

public actual val yandexDeveloperToken: String
    get() = BuildConfig.YANDEX_API_KEY