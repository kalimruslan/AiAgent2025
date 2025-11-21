package ru.llm.agent

/**
 * Desktop реализация запуска процесса через ProcessBuilder
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

    return processBuilder.start()
}