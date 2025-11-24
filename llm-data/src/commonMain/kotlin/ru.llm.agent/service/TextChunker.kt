package ru.llm.agent.service

/**
 * Сервис для разбиения текста на чанки (chunks)
 * Используется для подготовки текста к эмбеддингу в RAG системе
 */
public class TextChunker(
    private val chunkSize: Int = 500,
    private val overlap: Int = 50
) {
    /**
     * Разбивает текст на чанки с перекрытием
     * @param text исходный текст
     * @return список чанков
     */
    public fun splitText(text: String): List<String> {
        val chunks = mutableListOf<String>()

        // Разбиваем по параграфам для лучшей семантической целостности
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }

        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()

            // Если параграф сам по себе больше chunkSize, разбиваем его
            if (trimmedParagraph.length > chunkSize) {
                // Сохраняем текущий чанк если он не пустой
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }

                // Разбиваем большой параграф на предложения
                val sentences = trimmedParagraph.split(Regex("[.!?]+\\s+"))
                var sentenceChunk = StringBuilder()

                for (sentence in sentences) {
                    if (sentenceChunk.length + sentence.length > chunkSize) {
                        if (sentenceChunk.isNotEmpty()) {
                            chunks.add(sentenceChunk.toString().trim())

                            // Добавляем overlap из конца предыдущего чанка
                            val overlapText = sentenceChunk.toString()
                                .takeLast(overlap)
                            sentenceChunk = StringBuilder(overlapText)
                        }
                    }
                    sentenceChunk.append(sentence).append(". ")
                }

                if (sentenceChunk.isNotEmpty()) {
                    chunks.add(sentenceChunk.toString().trim())
                }

                continue
            }

            // Если добавление параграфа превысит chunkSize
            if (currentChunk.length + trimmedParagraph.length > chunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())

                    // Добавляем overlap
                    val overlapText = currentChunk.toString().takeLast(overlap)
                    currentChunk = StringBuilder(overlapText + "\n\n")
                }
            }

            currentChunk.append(trimmedParagraph).append("\n\n")
        }

        // Добавляем последний чанк
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }

    /**
     * Разбивает текст на чанки фиксированного размера без учёта семантики
     * Используется как fallback для очень длинных блоков
     */
    private fun splitBySize(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + chunkSize, text.length)
            chunks.add(text.substring(startIndex, endIndex))
            startIndex += chunkSize - overlap
        }

        return chunks
    }
}