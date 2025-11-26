package ru.llm.agent.service

import ru.llm.agent.api.OllamaApi

/**
 * Сервис для генерации эмбеддингов текста
 * Объединяет работу с Ollama API, TextChunker и VectorStore
 */
public class EmbeddingService(
    private val ollamaApi: OllamaApi,
    private val textChunker: TextChunker,
    private val vectorStore: VectorStore
) {
    /**
     * Индексирует текст: разбивает на чанки, генерирует эмбеддинги, сохраняет в векторное хранилище
     * @param text текст для индексации
     * @param sourceId идентификатор источника (например, имя файла)
     * @return количество проиндексированных чанков
     */
    public suspend fun indexText(text: String, sourceId: String): Int {
        // Разбиваем текст на чанки
        val chunks = textChunker.splitText(text)

        // Генерируем эмбеддинги для каждого чанка
        val documents = chunks.mapIndexed { index, chunk ->
            val embeddingResponse = ollamaApi.getEmbedding(chunk)

            Document(
                id = "$sourceId-chunk-$index",
                text = chunk,
                embedding = embeddingResponse.getEmbedding(),
                metadata = mapOf(
                    "source" to sourceId,
                    "chunk_index" to index.toString(),
                    "model" to embeddingResponse.getModelName()
                )
            ) to embeddingResponse.getModelName()
        }

        // Сохраняем все документы одним батчем
        if (documents.isNotEmpty()) {
            vectorStore.addDocuments(
                docs = documents.map { it.first },
                sourceId = sourceId,
                model = documents.first().second
            )
        }

        return chunks.size
    }

    /**
     * Поиск релевантных документов по запросу
     * @param query текст запроса
     * @param topK количество результатов
     * @param threshold минимальный порог схожести
     * @return список найденных документов
     */
    public suspend fun search(
        query: String,
        topK: Int = 5,
        threshold: Double = 0.3
    ): List<SearchResult> {
        // Генерируем эмбеддинг для запроса
        val queryEmbedding = ollamaApi.getEmbedding(query)

        // Ищем похожие документы
        return vectorStore.search(
            queryEmbedding = queryEmbedding.getEmbedding(),
            topK = topK,
            threshold = threshold
        )
    }

    /**
     * Поиск с использованием MMR (Maximum Marginal Relevance)
     * Обеспечивает баланс между релевантностью и разнообразием результатов
     *
     * @param query текст запроса
     * @param topK количество результатов
     * @param threshold минимальный порог схожести
     * @param lambda параметр баланса (0.0 = разнообразие, 1.0 = релевантность)
     * @return список найденных документов с MMR-ранжированием
     */
    public suspend fun searchWithMmr(
        query: String,
        topK: Int = 5,
        threshold: Double = 0.3,
        lambda: Double = 0.5
    ): List<SearchResult> {
        // Генерируем эмбеддинг для запроса
        val queryEmbedding = ollamaApi.getEmbedding(query)

        // Ищем с использованием MMR
        return vectorStore.searchWithMMR(
            queryEmbedding = queryEmbedding.getEmbedding(),
            topK = topK,
            threshold = threshold,
            lambda = lambda
        )
    }

    /**
     * Получить количество проиндексированных документов
     */
    public suspend fun getIndexedDocumentsCount(): Int = vectorStore.size()

    /**
     * Очистить индекс
     */
    public suspend fun clearIndex() {
        vectorStore.clear()
    }
}