package ru.llm.agent.core.utils

/**
 * Результат парсинга документа
 */
public sealed class DocumentParseResult {
    /** Текст успешно извлечен */
    public data class Success(val text: String) : DocumentParseResult()

    /** Ошибка при парсинге */
    public data class Error(val message: String) : DocumentParseResult()

    /** Неподдерживаемый формат */
    public data class UnsupportedFormat(val extension: String) : DocumentParseResult()
}

/**
 * Парсер документов для извлечения текста
 */
public interface DocumentParser {
    /**
     * Извлечь текст из байтов документа
     *
     * @param content Содержимое файла в байтах
     * @param fileName Имя файла (используется для определения формата)
     * @return Результат парсинга
     */
    public suspend fun extractText(
        content: ByteArray,
        fileName: String
    ): DocumentParseResult
}

/**
 * Получить платформозависимый парсер документов
 */
public expect fun getDocumentParser(): DocumentParser