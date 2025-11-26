package ru.llm.agent.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.llm.agent.database.rag.RagDocumentDao
import ru.llm.agent.database.rag.RagDocumentEntity
import kotlin.math.sqrt

/**
 * Документ с эмбеддингом
 */
public data class Document(
    val id: String,
    val text: String,
    val embedding: List<Double>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Результат поиска
 */
public data class SearchResult(
    val document: Document,
    val similarity: Double,
    /** MMR score (если использовался MMR алгоритм) */
    val mmrScore: Double? = null
)

/**
 * Персистентное векторное хранилище для RAG системы
 * Использует cosine similarity для поиска похожих документов
 * Сохраняет документы в Room базу данных
 */
public class VectorStore(
    private val ragDocumentDao: RagDocumentDao
) {
    private val json = Json

    /**
     * Добавить документ в хранилище
     */
    public suspend fun addDocument(document: Document, sourceId: String, chunkIndex: Int, model: String) {
        val entity = RagDocumentEntity(
            id = document.id,
            text = document.text,
            embedding = json.encodeToString(document.embedding),
            sourceId = sourceId,
            chunkIndex = chunkIndex,
            model = model
        )
        ragDocumentDao.insert(entity)
    }

    /**
     * Добавить несколько документов
     */
    public suspend fun addDocuments(
        docs: List<Document>,
        sourceId: String,
        model: String
    ) {
        val entities = docs.mapIndexed { index, doc ->
            RagDocumentEntity(
                id = doc.id,
                text = doc.text,
                embedding = json.encodeToString(doc.embedding),
                sourceId = sourceId,
                chunkIndex = index,
                model = model
            )
        }
        ragDocumentDao.insertAll(entities)
    }

    /**
     * Поиск наиболее похожих документов
     * @param queryEmbedding эмбеддинг запроса
     * @param topK количество результатов
     * @param threshold минимальный порог схожести (0.0 - 1.0)
     * @return список найденных документов с оценкой схожести
     */
    public suspend fun search(
        queryEmbedding: List<Double>,
        topK: Int = 5,
        threshold: Double = 0.0
    ): List<SearchResult> {
        // Загружаем все документы из БД
        val entities = ragDocumentDao.getAllDocuments()

        // Конвертируем в Document
        val documents = entities.map { entity ->
            Document(
                id = entity.id,
                text = entity.text,
                embedding = json.decodeFromString<List<Double>>(entity.embedding),
                metadata = mapOf(
                    "sourceId" to entity.sourceId,
                    "chunkIndex" to entity.chunkIndex.toString(),
                    "model" to entity.model
                )
            )
        }

        // Ищем похожие
        return documents
            .map { doc ->
                SearchResult(
                    document = doc,
                    similarity = cosineSimilarity(queryEmbedding, doc.embedding)
                )
            }
            .filter { it.similarity >= threshold }
            .sortedByDescending { it.similarity }
            .take(topK)
    }

    /**
     * Поиск с использованием MMR (Maximum Marginal Relevance)
     * Обеспечивает баланс между релевантностью и разнообразием результатов
     *
     * MMR = λ * similarity(doc, query) - (1-λ) * max(similarity(doc, selected_docs))
     *
     * @param queryEmbedding эмбеддинг запроса
     * @param topK количество результатов
     * @param threshold минимальный порог схожести (0.0 - 1.0)
     * @param lambda параметр баланса (0.0 = только разнообразие, 1.0 = только релевантность)
     * @param candidateMultiplier множитель для предварительной выборки кандидатов (topK * multiplier)
     * @return список найденных документов с оценкой MMR
     */
    public suspend fun searchWithMMR(
        queryEmbedding: List<Double>,
        topK: Int = 5,
        threshold: Double = 0.0,
        lambda: Double = 0.5,
        candidateMultiplier: Int = 3
    ): List<SearchResult> {
        // Загружаем все документы из БД
        val entities = ragDocumentDao.getAllDocuments()

        if (entities.isEmpty()) return emptyList()

        // Конвертируем в Document с предвычисленной схожестью к запросу
        val documentsWithSimilarity = entities.map { entity ->
            val embedding = json.decodeFromString<List<Double>>(entity.embedding)
            val doc = Document(
                id = entity.id,
                text = entity.text,
                embedding = embedding,
                metadata = mapOf(
                    "sourceId" to entity.sourceId,
                    "chunkIndex" to entity.chunkIndex.toString(),
                    "model" to entity.model
                )
            )
            doc to cosineSimilarity(queryEmbedding, embedding)
        }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }

        if (documentsWithSimilarity.isEmpty()) return emptyList()

        // Берём топ-N кандидатов для MMR (для эффективности)
        val candidates = documentsWithSimilarity
            .take(topK * candidateMultiplier)
            .toMutableList()

        val selected = mutableListOf<SearchResult>()

        // Выбираем первый документ — с наивысшей релевантностью
        val first = candidates.removeAt(0)
        selected.add(
            SearchResult(
                document = first.first,
                similarity = first.second,
                mmrScore = first.second // Для первого документа MMR = similarity
            )
        )

        // Итеративно выбираем остальные документы по MMR
        while (selected.size < topK && candidates.isNotEmpty()) {
            var bestMmrScore = Double.MIN_VALUE
            var bestIndex = -1

            for (i in candidates.indices) {
                val (candidateDoc, querySimilarity) = candidates[i]

                // Находим максимальную схожесть с уже выбранными документами
                val maxSimilarityToSelected = selected.maxOf { selectedResult ->
                    cosineSimilarity(candidateDoc.embedding, selectedResult.document.embedding)
                }

                // Вычисляем MMR score
                val mmrScore = lambda * querySimilarity - (1 - lambda) * maxSimilarityToSelected

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore
                    bestIndex = i
                }
            }

            if (bestIndex >= 0) {
                val (bestDoc, bestSimilarity) = candidates.removeAt(bestIndex)
                selected.add(
                    SearchResult(
                        document = bestDoc,
                        similarity = bestSimilarity,
                        mmrScore = bestMmrScore
                    )
                )
            } else {
                break
            }
        }

        return selected
    }

    /**
     * Вычисление косинусной схожести между двумя векторами
     * Возвращает значение от -1 до 1, где 1 означает полную схожесть
     */
    private fun cosineSimilarity(vec1: List<Double>, vec2: List<Double>): Double {
        require(vec1.size == vec2.size) { "Vectors must have the same dimension" }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)

        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }

    /**
     * Получить количество документов в хранилище
     */
    public suspend fun size(): Int = ragDocumentDao.getDocumentsCount()

    /**
     * Очистить хранилище
     */
    public suspend fun clear() {
        ragDocumentDao.deleteAll()
    }

    /**
     * Получить все документы
     */
    public suspend fun getAllDocuments(): List<Document> {
        val entities = ragDocumentDao.getAllDocuments()
        return entities.map { entity ->
            Document(
                id = entity.id,
                text = entity.text,
                embedding = json.decodeFromString<List<Double>>(entity.embedding),
                metadata = mapOf(
                    "sourceId" to entity.sourceId,
                    "chunkIndex" to entity.chunkIndex.toString(),
                    "model" to entity.model
                )
            )
        }
    }
}