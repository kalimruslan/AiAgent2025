package ru.llm.agent.database.rag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO для работы с RAG документами
 */
@Dao
public interface RagDocumentDao {
    /**
     * Добавить или обновить документ
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(document: RagDocumentEntity)

    /**
     * Добавить список документов
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(documents: List<RagDocumentEntity>)

    /**
     * Получить все документы
     */
    @Query("SELECT * FROM rag_documents ORDER BY timestamp DESC")
    public suspend fun getAllDocuments(): List<RagDocumentEntity>

    /**
     * Получить документы по источнику
     */
    @Query("SELECT * FROM rag_documents WHERE sourceId = :sourceId ORDER BY chunkIndex")
    public suspend fun getDocumentsBySource(sourceId: String): List<RagDocumentEntity>

    /**
     * Получить количество документов
     */
    @Query("SELECT COUNT(*) FROM rag_documents")
    public suspend fun getDocumentsCount(): Int

    /**
     * Удалить все документы
     */
    @Query("DELETE FROM rag_documents")
    public suspend fun deleteAll()

    /**
     * Удалить документы по источнику
     */
    @Query("DELETE FROM rag_documents WHERE sourceId = :sourceId")
    public suspend fun deleteBySource(sourceId: String)

    /**
     * Проверить существование документа
     */
    @Query("SELECT EXISTS(SELECT 1 FROM rag_documents WHERE id = :id)")
    public suspend fun exists(id: String): Boolean
}