package ru.llm.agent.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64

/**
 * Desktop-реализация FileManager
 */
private class DesktopFileManager : FileManager {
    override suspend fun saveFile(
        fileName: String,
        content: String,
        mimeType: String
    ): FileSaveResult = withContext(Dispatchers.IO) {
        try {
            // Используем стандартный диалог сохранения файла
            val fileDialog = FileDialog(null as Frame?, "Сохранить диалог", FileDialog.SAVE)
            fileDialog.file = fileName
            fileDialog.isVisible = true

            val selectedFile = fileDialog.file
            val selectedDir = fileDialog.directory

            if (selectedFile == null || selectedDir == null) {
                return@withContext FileSaveResult.Cancelled
            }

            val file = File(selectedDir, selectedFile)

            // Если контент закодирован в Base64 (например, PDF), декодируем его
            if (content.isBase64Encoded()) {
                val bytes = Base64.getDecoder().decode(content)
                file.writeBytes(bytes)
            } else {
                file.writeText(content)
            }

            FileSaveResult.Success(file.absolutePath)
        } catch (e: Exception) {
            FileSaveResult.Error("Ошибка сохранения файла: ${e.message}")
        }
    }
}

public actual fun getFileManager(): FileManager {
    return DesktopFileManager()
}