package ru.llm.agent.database.rag

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения проиндексированных документов с их embeddings
 */
@Entity(tableName = "rag_documents")
public data class RagDocumentEntity(
    @PrimaryKey
    val id: String,

    /** Текст документа/чанка */
    val text: String,

    /** Вектор эмбеддинга в виде строки (JSON массив) */
    val embedding: String,

    /** ID источника (например, имя файла) */
    val sourceId: String,

    /** Индекс чанка в источнике */
    val chunkIndex: Int,

    /** Модель эмбеддинга */
    val model: String,

    /** Временная метка создания */
    val timestamp: Long = System.currentTimeMillis()
)