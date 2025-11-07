package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.llm.agent.McpClient
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.createHttpClient
import ru.llm.agent.api.YandexApi
import ru.llm.agent.database.DatabaseDriverFactory
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ConversationRepositoryImpl
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LlmRepositoryImpl
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.repository.LocalDbRepositoryImpl
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.repository.McpRepositoryImpl

public expect val yandexDeveloperToken: String

public expect val proxyApiToken: String

public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            yandexApi = get(),
            proxyApi = get()
        )
    }
    single<ConversationRepository> {
        ConversationRepositoryImpl(
            yandexApi = get(),
            proxyApi = get(),
            messageDao = get<MessageDatabase>().messageDao(),
            contextDao = get<MessageDatabase>().settingsDao()
        )
    }
    single<LocalDbRepository>{
        LocalDbRepositoryImpl(
            contextDao = get<MessageDatabase>().settingsDao()
        )
    }

    single<McpRepository>{
        McpRepositoryImpl(
            mcpClient = get()
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

    single<McpClient> {
        McpClient(
            //serverUrl = "http://193.42.124.133/mcp",
            serverUrl = "http://10.0.2.2:8080/mcp",
            client = get(named("Yandex"))
        )
    }
}

public val databaseModule: Module = module {
    single<MessageDatabase> { get<DatabaseDriverFactory>().createDatabase() }
}

