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
import ru.llm.agent.repository.ExpertRepository
import ru.llm.agent.repository.ExpertRepositoryImpl
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LlmRepositoryImpl
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.repository.LocalDbRepositoryImpl
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.repository.McpRepositoryImpl
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.repository.ProviderConfigRepositoryImpl
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.service.MessageSendingServiceImpl

public expect val yandexDeveloperToken: String

public expect val proxyApiToken: String

public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            yandexApi = get(),
            proxyApi = get()
        )
    }

    // Новый репозиторий для управления конфигурацией провайдеров
    single<ProviderConfigRepository> {
        ProviderConfigRepositoryImpl(
            contextDao = get<MessageDatabase>().settingsDao()
        )
    }

    // Упрощенный ConversationRepository - только CRUD операции
    single<ConversationRepository> {
        ConversationRepositoryImpl(
            messageDao = get<MessageDatabase>().messageDao(),
            contextDao = get<MessageDatabase>().settingsDao(),
            expertOpinionDao = get<MessageDatabase>().expertOpinionDao(),
            providerConfigRepository = get<ProviderConfigRepository>(),
            systemPromptBuilder = get(),
            logger = ru.llm.agent.core.utils.createLogger("ConversationRepository")
        )
    }

    single<LocalDbRepository> {
        LocalDbRepositoryImpl(
            contextDao = get<MessageDatabase>().settingsDao()
        )
    }

    single<McpRepository> {
        McpRepositoryImpl(
            mcpClient = get()
        )
    }

    single<ExpertRepository> {
        ExpertRepositoryImpl(
            expertOpinionDao = get<MessageDatabase>().expertOpinionDao()
        )
    }
}

// Модуль для сервисов
public val servicesModule: Module = module {
    single<MessageSendingService> {
        MessageSendingServiceImpl(
            yandexApi = get(),
            proxyApi = get(),
            parseAssistantResponseUseCase = get(),
            logger = ru.llm.agent.core.utils.createLogger("MessageSendingService")
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

    single<HttpClient>(named("ProxyApiOpenAI")) {
        createHttpClient(
            developerToken = proxyApiToken,
            baseUrl = "https://api.proxyapi.ru/openai/v1/"
        )
    }

    single<HttpClient>(named("ProxyApiOpenRouter")) {
        createHttpClient(
            developerToken = proxyApiToken,
            baseUrl = "https://api.proxyapi.ru/openrouter/v1/"
        )
    }

    single<YandexApi> { YandexApi(httpClient = get(named("Yandex"))) }
    single<ProxyApi> {
        ProxyApi(
            httpClientOpenAi = get(named("ProxyApiOpenAI")),
            httpClientOpenRouter = get(named("ProxyApiOpenRouter"))
        )
    }

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

