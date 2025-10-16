package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.createHttpClient
import ru.llm.agent.api.YandexApi
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LlmRepositoryImpl

public expect val yandexDeveloperToken: String

public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            yandexApi = get(),
        )
    }
}

public val networkModule: Module = module {
    single<HttpClient>(named("Yandex")) {
        createHttpClient(
            developerToken = yandexDeveloperToken,
            baseUrl = "https://llm.api.cloud.yandex.net/"
        )
    }

    single<YandexApi> { YandexApi(httpClient = get(named("Yandex"))) }
}
