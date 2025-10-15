package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.createHttpClient
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.api.YandexApi
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LlmRepositoryImpl

public expect val yandexDeveloperToken: String
public expect val proxyApiToken: String

public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            proxyApi = get(),
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

    single<HttpClient>(named("ProxyApi")) {
        createHttpClient(
            developerToken = proxyApiToken,
            baseUrl = "https://api.proxyapi.ru/openai/v1/"
        )
    }

    single<YandexApi> { YandexApi(httpClient = get(named("Yandex"))) }
    single<ProxyApi> { ProxyApi(httpClient = get(named("ProxyApi"))) }
}
