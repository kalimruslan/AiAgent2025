package ru.llm.agent.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/**
 * Типизированные квалификаторы для HttpClient.
 * Предотвращает использование магических строк в DI конфигурации.
 */
public object HttpClientQualifier {
    /**
     * HttpClient для Yandex GPT API
     */
    public val Yandex: Qualifier = named("Yandex")

    /**
     * HttpClient для ProxyAPI (OpenAI)
     */
    public val ProxyApiOpenAI: Qualifier = named("ProxyApiOpenAI")

    /**
     * HttpClient для ProxyAPI (OpenRouter)
     */
    public val ProxyApiOpenRouter: Qualifier = named("ProxyApiOpenRouter")
}