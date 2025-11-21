package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.api.ProxyApi
import ru.llm.agent.createHttpClient
import ru.llm.agent.api.YandexApi
import ru.llm.agent.database.DatabaseDriverFactory
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.model.config.ProviderConfig
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.repository.ConversationRepositoryImpl
import ru.llm.agent.repository.ExpertRepository
import ru.llm.agent.repository.ExpertRepositoryImpl
import ru.llm.agent.repository.LlmConfigRepository
import ru.llm.agent.repository.LlmConfigRepositoryImpl
import ru.llm.agent.repository.LlmRepository
import ru.llm.agent.repository.LlmRepositoryImpl
import ru.llm.agent.repository.LocalDbRepository
import ru.llm.agent.repository.LocalDbRepositoryImpl
import ru.llm.agent.repository.McpRepository
import ru.llm.agent.repository.McpRepositoryImpl
import ru.llm.agent.repository.McpServerRepository
import ru.llm.agent.repository.McpServerRepositoryImpl
import ru.llm.agent.repository.ProviderConfigRepository
import ru.llm.agent.repository.ProviderConfigRepositoryImpl
import ru.llm.agent.exporter.ConversationExporter
import ru.llm.agent.exporter.JsonConversationExporter
import ru.llm.agent.exporter.PdfConversationExporter
import ru.llm.agent.service.MessageSendingService
import ru.llm.agent.service.MessageSendingServiceImpl

public expect val yandexDeveloperToken: String

public expect val proxyApiToken: String

public val repositoriesModule: Module = module {
    single<LlmRepository> {
        LlmRepositoryImpl(
            yandexApi = get(),
            proxyApi = get(),
            llmConfigRepository = get()
        )
    }

    // Репозиторий для управления конфигурацией LLM моделей
    single<LlmConfigRepository> {
        LlmConfigRepositoryImpl()
    }

    // Репозиторий для управления конфигурацией провайдеров
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

    single<ru.llm.agent.McpClientsManager> {
        ru.llm.agent.McpClientsManager(
            mcpServerRepository = get(),
            httpClient = get(HttpClientQualifier.Yandex)
        )
    }

    single<McpRepository> {
        McpRepositoryImpl(
            mcpClientsManager = get()
        )
    }

    single<ExpertRepository> {
        ExpertRepositoryImpl(
            expertOpinionDao = get<MessageDatabase>().expertOpinionDao()
        )
    }

    // Новые специализированные repository (после рефакторинга)
    single<ru.llm.agent.repository.MessageRepository> {
        ru.llm.agent.repository.MessageRepositoryImpl(
            database = get<MessageDatabase>()
        )
    }

    single<ru.llm.agent.repository.TokenManagementRepository> {
        ru.llm.agent.repository.TokenManagementRepositoryImpl(
            database = get<MessageDatabase>()
        )
    }

    single<ru.llm.agent.repository.SummarizationRepository> {
        ru.llm.agent.repository.SummarizationRepositoryImpl(
            database = get<MessageDatabase>()
        )
    }

    single<McpServerRepository> {
        McpServerRepositoryImpl(
            database = get<MessageDatabase>()
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
    // Конфигурации провайдеров
    val yandexConfig = ProviderConfig.yandexGpt()
    val proxyGpt4oConfig = ProviderConfig.proxyApiGpt4oMini()
    val proxyMistralConfig = ProviderConfig.proxyApiMistralAi()

    // HttpClient для YandexGPT
    single<HttpClient>(HttpClientQualifier.Yandex) {
        createHttpClient(
            developerToken = yandexDeveloperToken,
            baseUrl = yandexConfig.baseUrl
        )
    }

    // HttpClient для ProxyAPI (OpenAI)
    single<HttpClient>(HttpClientQualifier.ProxyApiOpenAI) {
        createHttpClient(
            developerToken = proxyApiToken,
            baseUrl = proxyGpt4oConfig.baseUrl
        )
    }

    // HttpClient для ProxyAPI (OpenRouter)
    single<HttpClient>(HttpClientQualifier.ProxyApiOpenRouter) {
        createHttpClient(
            developerToken = proxyApiToken,
            baseUrl = proxyMistralConfig.baseUrl
        )
    }

    // API клиенты
    single<YandexApi> { YandexApi(httpClient = get(HttpClientQualifier.Yandex)) }
    single<ProxyApi> {
        ProxyApi(
            httpClientOpenAi = get(HttpClientQualifier.ProxyApiOpenAI),
            httpClientOpenRouter = get(HttpClientQualifier.ProxyApiOpenRouter)
        )
    }
}

public val databaseModule: Module = module {
    single<MessageDatabase> { get<DatabaseDriverFactory>().createDatabase() }
}

// Модуль для экспортеров диалогов
public val exportersModule: Module = module {
    single<ConversationExporter> { JsonConversationExporter() }
    single<ConversationExporter> { PdfConversationExporter() }
}
