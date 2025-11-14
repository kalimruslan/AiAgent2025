package ru.llm.agent.exporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDFont
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

/**
 * Desktop-реализация экспортера в PDF
 * Использует Apache PDFBox
 */
public actual class PdfConversationExporter actual constructor() : ConversationExporter {
    actual override val format: ExportFormat = ExportFormat.PDF

    private val margin = 50f
    private val lineHeight = 15f
    private val fontSize = 12f
    private val titleFontSize = 18f

    actual override suspend fun export(
        conversationId: String,
        messages: List<ConversationMessage>
    ): String = withContext(Dispatchers.IO) {
        val document = PDDocument()

        try {
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)
            var currentY = page.mediaBox.height - margin

            // Загружаем шрифт, поддерживающий кириллицу
            val font = loadFont(document)
            val boldFont = font // Используем тот же шрифт для жирного (PDType0Font не имеет bold варианта)

            // Заголовок
            contentStream.beginText()
            contentStream.setFont(boldFont, titleFontSize)
            contentStream.newLineAtOffset(margin, currentY)
            contentStream.showText("Экспорт диалога")
            contentStream.endText()
            currentY -= lineHeight * 2

            // Информация
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru"))
            contentStream.beginText()
            contentStream.setFont(font, 10f)
            contentStream.newLineAtOffset(margin, currentY)
            contentStream.showText("ID: $conversationId")
            contentStream.endText()
            currentY -= lineHeight

            contentStream.beginText()
            contentStream.setFont(font, 10f)
            contentStream.newLineAtOffset(margin, currentY)
            contentStream.showText("Дата экспорта: ${dateFormat.format(Date())}")
            contentStream.endText()
            currentY -= lineHeight * 2

            // Отрисовываем сообщения
            messages.forEach { message ->
                // Пропускаем системные и служебные сообщения
                if (message.role == Role.SYSTEM || message.role == Role.NONE) return@forEach

                // Проверяем, нужна ли новая страница
                if (currentY < margin + lineHeight * 5) {
                    contentStream.close()
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    currentY = page.mediaBox.height - margin
                }

                // Роль
                val roleText = when (message.role) {
                    Role.USER -> "Пользователь:"
                    Role.ASSISTANT -> "Ассистент:"
                    Role.SYSTEM -> "Система:"
                    Role.FUNCTION -> "Функция:"
                    Role.NONE -> "" // Не должно сюда попасть из-за проверки выше
                }

                contentStream.beginText()
                contentStream.setFont(boldFont, fontSize)
                contentStream.newLineAtOffset(margin, currentY)
                contentStream.showText(roleText)
                contentStream.endText()
                currentY -= lineHeight

                // Текст сообщения (с переносом строк)
                val maxWidth = page.mediaBox.width - margin * 2
                val textLines = wrapText(message.text, maxWidth, font, fontSize)

                textLines.forEach { line ->
                    // Проверяем, нужна ли новая страница
                    if (currentY < margin + lineHeight) {
                        contentStream.close()
                        page = PDPage(PDRectangle.A4)
                        document.addPage(page)
                        contentStream = PDPageContentStream(document, page)
                        currentY = page.mediaBox.height - margin
                    }

                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.newLineAtOffset(margin, currentY)
                    contentStream.showText(line)
                    contentStream.endText()
                    currentY -= lineHeight
                }

                // Информация о токенах (если есть)
                if (message.totalTokens != null) {
                    contentStream.beginText()
                    contentStream.setFont(font, 10f)
                    contentStream.newLineAtOffset(margin, currentY)
                    contentStream.showText("Токены: ${message.totalTokens}")
                    contentStream.endText()
                    currentY -= lineHeight
                }

                currentY -= lineHeight // Отступ между сообщениями
            }

            contentStream.close()

            // Сохраняем PDF в байты и кодируем в Base64
            val outputStream = ByteArrayOutputStream()
            document.save(outputStream)

            Base64.getEncoder().encodeToString(outputStream.toByteArray())
        } finally {
            document.close()
        }
    }

    /**
     * Загружает шрифт с поддержкой кириллицы
     */
    private fun loadFont(document: PDDocument): PDFont {
        return try {
            // Пытаемся загрузить системный шрифт Arial или DejaVu
            val fontPaths = listOf(
                "/System/Library/Fonts/Supplemental/Arial.ttf", // macOS
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", // Linux
                "C:\\Windows\\Fonts\\arial.ttf" // Windows
            )

            val fontFile = fontPaths.map { File(it) }.firstOrNull { it.exists() }

            if (fontFile != null) {
                PDType0Font.load(document, fontFile)
            } else {
                // Если системный шрифт не найден, используем встроенный (без кириллицы)
                // В этом случае транслитерируем текст
                org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
            }
        } catch (e: Exception) {
            // Fallback на стандартный шрифт
            org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
        }
    }

    /**
     * Разбивает текст на строки с учётом ширины
     */
    private fun wrapText(
        text: String,
        maxWidth: Float,
        font: PDFont,
        fontSize: Float
    ): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = font.getStringWidth(testLine) / 1000 * fontSize

            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}