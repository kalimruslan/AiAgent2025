package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
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

public expect val yandexDeveloperToken: String
public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            yandexApi = get(),
        )
    }
    single<ConversationRepository> {
        ConversationRepositoryImpl(
            yandexApi = get(),
            messageDao = get<MessageDatabase>().messageDao(),
            settingsDao = get<MessageDatabase>().settingsDao()
        )
    }
    single<LocalDbRepository>{
        LocalDbRepositoryImpl(
            settingsDao = get<MessageDatabase>().settingsDao()
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

public val databaseModule: Module = module {
    single<MessageDatabase> { get<DatabaseDriverFactory>().createDatabase() }
}

