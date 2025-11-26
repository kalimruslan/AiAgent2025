package ru.llm.agent.repository

import ru.llm.agent.model.rag.RagDocument
import ru.llm.agent.model.rag.RagIndexResult

/**
 * Repository для работы с RAG (Retrieval-Augmented Generation)
 */
public interface RagRepository {
    /**
     * Индексировать текст для последующего поиска
     * @param text текст для индексации
     * @param sourceId идентификатор источника
     * @return результат индексации
     */
    public suspend fun indexText(text: String, sourceId: String): RagIndexResult

    /**
     * Найти релевантные документы по запросу
     * @param query поисковый запрос
     * @param topK количество результатов
     * @param threshold минимальный порог схожести (0.0 - 1.0)
     * @param useMmr использовать MMR для разнообразия результатов
     * @param mmrLambda параметр баланса MMR (0.0 = разнообразие, 1.0 = релевантность)
     * @return список найденных документов
     */
    public suspend fun search(
        query: String,
        topK: Int = 5,
        threshold: Double = 0.3,
        useMmr: Boolean = true,
        mmrLambda: Double = 0.5
    ): List<RagDocument>

    /**
     * Получить количество проиндексированных документов
     */
    public suspend fun getIndexedCount(): Int

    /**
     * Очистить индекс
     */
    public suspend fun clearIndex()
}