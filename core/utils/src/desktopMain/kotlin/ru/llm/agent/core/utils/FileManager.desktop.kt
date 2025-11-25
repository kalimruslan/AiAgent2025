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

    override suspend fun pickFile(
        allowedExtensions: List<String>
    ): FilePickResult = withContext(Dispatchers.IO) {
        try {
            // Используем стандартный диалог выбора файла
            val fileDialog = FileDialog(null as Frame?, "Выбрать файл", FileDialog.LOAD)

            // Устанавливаем фильтр по расширениям, если указаны
            if (allowedExtensions.isNotEmpty()) {
                fileDialog.setFilenameFilter { _, name ->
                    allowedExtensions.any { ext ->
                        name.lowercase().endsWith(".$ext")
                    }
                }
            }

            fileDialog.isVisible = true

            val selectedFile = fileDialog.file
            val selectedDir = fileDialog.directory

            if (selectedFile == null || selectedDir == null) {
                return@withContext FilePickResult.Cancelled
            }

            val file = File(selectedDir, selectedFile)

            if (!file.exists()) {
                return@withContext FilePickResult.Error("Файл не найден")
            }

            if (!file.canRead()) {
                return@withContext FilePickResult.Error("Нет доступа к файлу")
            }

            val content = file.readBytes()

            FilePickResult.Success(
                filePath = file.absolutePath,
                fileName = file.name,
                content = content
            )
        } catch (e: Exception) {
            FilePickResult.Error("Ошибка выбора файла: ${e.message}")
        }
    }
}

public actual fun getFileManager(): FileManager {
    return DesktopFileManager()
}