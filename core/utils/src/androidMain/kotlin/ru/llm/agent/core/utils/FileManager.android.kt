package ru.llm.agent.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

/**
 * Android-реализация FileManager
 *
 * ВАЖНО: Для выбора файлов на Android необходимо использовать Activity Result API
 * из UI слоя. Эта реализация предоставляет вспомогательные методы для работы с выбранными Uri.
 */
private class AndroidFileManager : FileManager {
    override suspend fun saveFile(
        fileName: String,
        content: String,
        mimeType: String
    ): FileSaveResult = withContext(Dispatchers.IO) {
        try {
            // Сохраняем файл в Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { output ->
                // Если контент закодирован в Base64 (например, PDF), декодируем его
                val bytes = if (content.isBase64Encoded()) {
                    Base64.decode(content, Base64.DEFAULT)
                } else {
                    content.toByteArray()
                }
                output.write(bytes)
            }

            FileSaveResult.Success(file.absolutePath)
        } catch (e: Exception) {
            FileSaveResult.Error("Ошибка сохранения файла: ${e.message}")
        }
    }

    override suspend fun pickFile(
        allowedExtensions: List<String>
    ): FilePickResult {
        // На Android выбор файлов должен происходить через Activity Result API
        // из UI слоя (Composable функция с rememberLauncherForActivityResult)
        return FilePickResult.Error(
            "Выбор файлов на Android должен происходить из UI слоя. " +
            "Используйте rememberLauncherForActivityResult с GetContent контрактом."
        )
    }
}

/**
 * Получить содержимое файла по Uri (вспомогательная функция для Android)
 *
 * @param context Android контекст
 * @param uri Uri выбранного документа
 * @return Результат чтения файла
 */
public suspend fun readFileFromUri(
    context: Context,
    uri: Uri
): FilePickResult = withContext(Dispatchers.IO) {
    try {
        val contentResolver = context.contentResolver

        // Получаем имя файла
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "unknown"

        // Читаем содержимое файла
        val content = contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return@withContext FilePickResult.Error("Не удалось прочитать файл")

        FilePickResult.Success(
            filePath = uri.toString(),
            fileName = fileName,
            content = content
        )
    } catch (e: Exception) {
        FilePickResult.Error("Ошибка чтения файла: ${e.message}")
    }
}

public actual fun getFileManager(): FileManager {
    return AndroidFileManager()
}