package ru.llm.agent.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.api.OllamaApi
import ru.llm.agent.repository.RagRepository
import ru.llm.agent.repository.RagRepositoryImpl
import ru.llm.agent.repository.RagSourceRepository
import ru.llm.agent.repository.RagSourceRepositoryImpl
import ru.llm.agent.service.EmbeddingService
import ru.llm.agent.service.TextChunker
import ru.llm.agent.service.VectorStore

/**
 * Koin модуль для RAG компонентов (data layer)
 */
public val ragModule: Module = module {
    // Ollama API клиент
    single {
        OllamaApi(
            client = get<HttpClient>(HttpClientQualifier.Yandex),
            baseUrl = "http://localhost:11434"
        )
    }

    // Text Chunker - сервис разбиения текста
    single {
        TextChunker(
            chunkSize = 500,  // Увеличили размер для mxbai-embed-large (поддерживает до 512 токенов)
            overlap = 100
        )
    }

    // Vector Store - персистентное хранилище векторов с Room базой данных
    single {
        VectorStore(
            ragDocumentDao = get<ru.llm.agent.database.MessageDatabase>().ragDocumentDao()
        )
    }

    // Embedding Service - сервис для работы с эмбеддингами
    single {
        EmbeddingService(
            ollamaApi = get(),
            textChunker = get(),
            vectorStore = get()
        )
    }

    // RAG Repository
    single<RagRepository> {
        RagRepositoryImpl(
            embeddingService = get()
        )
    }

    // RAG Source Repository (для хранения источников ответов)
    single<RagSourceRepository> {
        RagSourceRepositoryImpl(
            ragSourceDao = get<ru.llm.agent.database.MessageDatabase>().ragSourceDao()
        )
    }
}