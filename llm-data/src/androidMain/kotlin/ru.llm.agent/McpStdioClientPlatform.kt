package ru.llm.agent

/**
 * Android реализация запуска процесса через ProcessBuilder.
 *
 * ВАЖНО: На Android есть ограничения:
 * - Нельзя запускать произвольные бинарники (только системные команды или из assets)
 * - Для запуска Node.js/Python нужны специальные решения (Termux, etc.)
 * - Полный путь к исполняемым файлам обязателен
 */
internal actual fun startProcess(
    command: String,
    args: List<String>,
    env: Map<String, String>?
): Process {
    val processBuilder = ProcessBuilder(listOf(command) + args)

    // Добавляем переменные окружения, если есть
    env?.let { envVars ->
        processBuilder.environment().putAll(envVars)
    }

    // Перенаправляем stderr для логирования ошибок
    processBuilder.redirectErrorStream(false)

    return try {
        processBuilder.start()
    } catch (e: Exception) {
        throw IllegalStateException(
            "Не удалось запустить процесс '$command'. " +
            "На Android требуется полный путь к исполняемому файлу. " +
            "Для Node.js/Python рассмотрите использование Termux API.",
            e
        )
    }
}