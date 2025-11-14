package ru.llm.agent.exporter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Base64

/**
 * Android-реализация экспортера в PDF
 * Использует android.graphics.pdf.PdfDocument
 */
public actual class PdfConversationExporter actual constructor() : ConversationExporter {
    actual override val format: ExportFormat = ExportFormat.PDF

    private val pageWidth = 595 // A4 ширина в точках
    private val pageHeight = 842 // A4 высота в точках
    private val margin = 40f
    private val lineHeight = 20f

    actual override suspend fun export(
        conversationId: String,
        messages: List<ConversationMessage>
    ): String = withContext(Dispatchers.Default) {
        val document = PdfDocument()
        var pageNumber = 1
        var currentY = margin

        // Создаём первую страницу
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Заголовок
        val titlePaint = TextPaint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        canvas.drawText("Экспорт диалога", margin, currentY, titlePaint)
        currentY += lineHeight * 2

        // ID диалога и дата
        val infoPaint = TextPaint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru"))
        canvas.drawText("ID: $conversationId", margin, currentY, infoPaint)
        currentY += lineHeight
        canvas.drawText("Дата экспорта: ${dateFormat.format(Date())}", margin, currentY, infoPaint)
        currentY += lineHeight * 2

        // Paint для сообщений
        val messagePaint = TextPaint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        val rolePaint = TextPaint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        // Отрисовываем сообщения
        messages.forEach { message ->
            // Пропускаем системные и служебные сообщения
            if (message.role == Role.SYSTEM || message.role == Role.NONE) return@forEach

            // Проверяем, нужна ли новая страница
            if (currentY > pageHeight - margin - lineHeight * 5) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }

            // Роль
            val roleText = when (message.role) {
                Role.USER -> "Пользователь:"
                Role.ASSISTANT -> "Ассистент:"
                Role.SYSTEM -> "Система:"
                Role.FUNCTION -> "Функция:"
                Role.NONE -> "" // Не должно сюда попасть из-за проверки выше
            }
            canvas.drawText(roleText, margin, currentY, rolePaint)
            currentY += lineHeight

            // Текст сообщения (с переносом строк)
            val textLines = wrapText(message.text, pageWidth - margin * 2, messagePaint)
            textLines.forEach { line ->
                // Проверяем, нужна ли новая страница
                if (currentY > pageHeight - margin - lineHeight) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin
                }

                canvas.drawText(line, margin, currentY, messagePaint)
                currentY += lineHeight
            }

            // Информация о токенах (если есть)
            if (message.totalTokens != null) {
                val tokenInfo = "Токены: ${message.totalTokens}"
                canvas.drawText(tokenInfo, margin, currentY, infoPaint)
                currentY += lineHeight
            }

            currentY += lineHeight // Отступ между сообщениями
        }

        document.finishPage(page)

        // Сохраняем PDF в байты и кодируем в Base64
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()

        Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    /**
     * Разбивает текст на строки с учётом ширины
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

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