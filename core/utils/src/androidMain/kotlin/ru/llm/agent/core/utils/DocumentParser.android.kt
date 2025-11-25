package ru.llm.agent.core.utils

/**
 * Android-реализация парсера документов
 *
 * ВАЖНО: Для полноценной работы с PDF и DOC на Android рекомендуется
 * использовать сторонние библиотеки или облачные сервисы парсинга.
 * Данная реализация предоставляет базовую поддержку через веб-сервисы
 * или placeholder для будущей интеграции.
 */
private class AndroidDocumentParser : DocumentParser {
    override suspend fun extractText(
        content: ByteArray,
        fileName: String
    ): DocumentParseResult {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "pdf", "doc", "docx" -> {
                // TODO: Добавить поддержку парсинга PDF/DOC на Android
                // Варианты реализации:
                // 1. Использовать PdfBox для Android (требует дополнительных зависимостей)
                // 2. Использовать облачный сервис для парсинга
                // 3. Использовать Intent для открытия в стороннем приложении и копирования текста
                DocumentParseResult.Error(
                    "Парсинг $extension документов на Android пока не реализован. " +
                    "Используйте desktop версию или текстовые форматы."
                )
            }
            else -> DocumentParseResult.UnsupportedFormat(extension)
        }
    }
}

public actual fun getDocumentParser(): DocumentParser {
    return AndroidDocumentParser()
}