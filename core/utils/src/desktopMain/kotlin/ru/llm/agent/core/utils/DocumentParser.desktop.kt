package ru.llm.agent.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream

/**
 * Desktop-реализация парсера документов
 * Использует Apache PDFBox для PDF и Apache POI для DOC/DOCX
 */
private class DesktopDocumentParser : DocumentParser {
    override suspend fun extractText(
        content: ByteArray,
        fileName: String
    ): DocumentParseResult = withContext(Dispatchers.IO) {
        try {
            val extension = fileName.substringAfterLast('.', "").lowercase()

            when (extension) {
                "pdf" -> extractFromPdf(content)
                "doc" -> extractFromDoc(content)
                "docx" -> extractFromDocx(content)
                else -> DocumentParseResult.UnsupportedFormat(extension)
            }
        } catch (e: Exception) {
            DocumentParseResult.Error("Ошибка парсинга документа: ${e.message}")
        }
    }

    /**
     * Извлечь текст из PDF файла
     */
    private fun extractFromPdf(content: ByteArray): DocumentParseResult {
        return try {
            PDDocument.load(content).use { document ->
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)

                if (text.isBlank()) {
                    DocumentParseResult.Error("PDF документ не содержит текста")
                } else {
                    DocumentParseResult.Success(text.trim())
                }
            }
        } catch (e: Exception) {
            DocumentParseResult.Error("Ошибка чтения PDF: ${e.message}")
        }
    }

    /**
     * Извлечь текст из DOC файла (старый формат Microsoft Word)
     */
    private fun extractFromDoc(content: ByteArray): DocumentParseResult {
        return try {
            ByteArrayInputStream(content).use { input ->
                val document = HWPFDocument(input)
                val extractor = WordExtractor(document)
                val text = extractor.text

                if (text.isBlank()) {
                    DocumentParseResult.Error("DOC документ не содержит текста")
                } else {
                    DocumentParseResult.Success(text.trim())
                }
            }
        } catch (e: Exception) {
            DocumentParseResult.Error("Ошибка чтения DOC: ${e.message}")
        }
    }

    /**
     * Извлечь текст из DOCX файла (новый формат Microsoft Word)
     */
    private fun extractFromDocx(content: ByteArray): DocumentParseResult {
        return try {
            ByteArrayInputStream(content).use { input ->
                val document = XWPFDocument(input)
                val extractor = XWPFWordExtractor(document)
                val text = extractor.text

                if (text.isBlank()) {
                    DocumentParseResult.Error("DOCX документ не содержит текста")
                } else {
                    DocumentParseResult.Success(text.trim())
                }
            }
        } catch (e: Exception) {
            DocumentParseResult.Error("Ошибка чтения DOCX: ${e.message}")
        }
    }
}

public actual fun getDocumentParser(): DocumentParser {
    return DesktopDocumentParser()
}