package ru.llm.agent.core.utils

/**
 * Результат сохранения файла
 */
public sealed class FileSaveResult {
    /** Файл успешно сохранен */
    public data class Success(val filePath: String) : FileSaveResult()

    /** Пользователь отменил сохранение */
    public object Cancelled : FileSaveResult()

    /** Ошибка при сохранении */
    public data class Error(val message: String) : FileSaveResult()
}

/**
 * Платформозависимый менеджер для работы с файлами
 */
public interface FileManager {
    /**
     * Сохранить файл с диалогом выбора пути
     *
     * @param fileName Имя файла по умолчанию
     * @param content Содержимое файла (для PDF - Base64 encoded)
     * @param mimeType MIME-тип файла
     * @return Результат сохранения
     */
    public suspend fun saveFile(
        fileName: String,
        content: String,
        mimeType: String
    ): FileSaveResult
}

/**
 * Проверить, является ли содержимое Base64-encoded бинарными данными
 */
public fun String.isBase64Encoded(): Boolean {
    return this.matches(Regex("^[A-Za-z0-9+/]*={0,2}$")) && this.length % 4 == 0
}

/**
 * Получить платформозависимый FileManager
 */
public expect fun getFileManager(): FileManager