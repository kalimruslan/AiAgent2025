package ru.llm.agent.core.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

/**
 * Android-реализация FileManager
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
}

public actual fun getFileManager(): FileManager {
    return AndroidFileManager()
}