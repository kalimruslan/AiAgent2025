package ru.llm.agent

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.json.*
import ru.llm.agent.api.YandexTrackerClient
import ru.llm.agent.api.OpenWeatherMapClient

/**
 * MCP Сервер на основе официального Kotlin SDK.
 * Предоставляет набор инструментов для взаимодействия с AI агентами.
 *
 * Поддерживает как SDK транспорт, так и HTTP JSON-RPC через дополнительный реестр.
 */
class McpServerSdk {

    private val server: Server

    /**
     * Реестр инструментов для HTTP доступа.
     * Хранит метаданные и обработчики tools для JSON-RPC.
     */
    private val toolsRegistry = mutableMapOf<String, RegisteredTool>()

    init {
        server = Server(
            serverInfo = Implementation(
                name = "llm-agent-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false)
                )
            )
        )

        registerTools()
    }

    /**
     * Клиент для работы с Яндекс.Трекером (lazy initialization)
     */
    private val trackerClient: YandexTrackerClient? by lazy {
        val orgId = System.getenv("YANDEX_TRACKER_ORG_ID")
        val token = System.getenv("YANDEX_TRACKER_TOKEN")

        if (orgId != null && token != null) {
            YandexTrackerClient(orgId, token)
        } else {
            println("WARN: Яндекс.Трекер не настроен. Установите YANDEX_TRACKER_ORG_ID и YANDEX_TRACKER_TOKEN")
            null
        }
    }

    /**
     * Клиент для работы с OpenWeatherMap (lazy initialization)
     */
    private val weatherClient: OpenWeatherMapClient? by lazy {
        val apiKey = "2e35cd4c8f78321391bf0b821be02145"
        if (apiKey != null) {
            OpenWeatherMapClient(apiKey)
        } else {
            println("WARN: OpenWeatherMap не настроен. Установите OPENWEATHER_API_KEY")
            null
        }
    }

    /**
     * Регистрирует все доступные инструменты на сервере
     */
    private fun registerTools() {
        // Базовые инструменты
        registerEchoTool()
        registerAddTool()
        registerGetCurrentTimeTool()
        registerGetWeatherTool()
        registerCalculateTool()

        // Яндекс.Трекер инструменты
        registerTrackerGetIssues()
        registerTrackerCreateIssue()
        registerTrackerGetIssue()
    }

    /**
     * Возвращает список всех зарегистрированных инструментов
     */
    fun getToolsList(): List<RegisteredTool> = toolsRegistry.values.toList()

    /**
     * Вызывает инструмент по имени с указанными аргументами
     */
    suspend fun callTool(name: String, arguments: JsonObject): CallToolResult {
        val tool = toolsRegistry[name]
            ?: throw IllegalArgumentException("Unknown tool: $name")

        return tool.handler(arguments)
    }

    /**
     * Зарегистрированный инструмент с метаданными
     */
    data class RegisteredTool(
        val name: String,
        val description: String,
        val inputSchema: Tool.Input,
        val handler: suspend (JsonObject) -> CallToolResult
    )

    /**
     * Вспомогательный метод для регистрации инструмента одновременно в SDK и HTTP реестре
     */
    private fun registerTool(
        name: String,
        description: String,
        inputSchema: Tool.Input,
        handler: suspend (JsonObject) -> CallToolResult
    ) {
        // Регистрируем в SDK
        server.addTool(name, description, inputSchema) { request ->
            handler(request.arguments)
        }

        // Регистрируем в HTTP реестре
        toolsRegistry[name] = RegisteredTool(name, description, inputSchema, handler)
    }

    /**
     * Инструмент для возврата введенного текста
     */
    private fun registerEchoTool() {
        registerTool(
            name = "echo",
            description = "Возвращает введенный текст",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Текст для echo")
                    }
                },
                required = listOf("text")
            )
        ) { arguments ->
            val text = arguments["text"]?.jsonPrimitive?.content ?: ""
            CallToolResult(
                content = listOf(
                    TextContent(text = "Echo: $text")
                )
            )
        }
    }

    /**
     * Инструмент для сложения двух чисел
     */
    private fun registerAddTool() {
        registerTool(
            name = "add",
            description = "Складывает два числа",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Введите выражение")
                    }
                },
                required = listOf("text")
            )
        ) { arguments ->
            val text = arguments["text"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'text' in arguments")

            val cleanText = text.replace(" ", "")
            val parts = cleanText.split("+")

            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid format. Expected 'a + b', got: $text")
            }

            val a = parts[0].toDoubleOrNull()
            val b = parts[1].toDoubleOrNull()

            if (a == null || b == null) {
                throw IllegalArgumentException("Invalid numbers: $text")
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = "Result: ${a + b}")
                )
            )
        }
    }

    /**
     * Инструмент для получения текущего времени
     */
    private fun registerGetCurrentTimeTool() {
        registerTool(
            name = "getCurrentTime",
            description = "Возвращает текущее время",
            inputSchema = Tool.Input(
                properties = buildJsonObject {}
            )
        ) { _ ->
            CallToolResult(
                content = listOf(
                    TextContent(text = "Current time: ${System.currentTimeMillis()}")
                )
            )
        }
    }

    /**
     * Инструмент для получения погоды
     */
    private fun registerGetWeatherTool() {
        registerTool(
            name = "getWeather",
            description = "Получает информацию о погоде для указанного города",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("city") {
                        put("type", "string")
                        put("description", "Название города для получения погоды")
                    }
                },
                required = listOf("city")
            )
        ) { arguments ->
            val city = arguments["city"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'city' in arguments")

            val weatherResult = getWeather(city)
            CallToolResult(
                content = listOf(
                    TextContent(text = weatherResult)
                )
            )
        }
    }

    /**
     * Инструмент для вычисления математических выражений
     */
    private fun registerCalculateTool() {
        registerTool(
            name = "calculate",
            description = "Вычисляет математическое выражение. Поддерживает +, -, *, /, скобки и числа с плавающей точкой",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("expression") {
                        put("type", "string")
                        put("description", "Математическое выражение для вычисления (например: '2 + 2', '(10 - 5) * 3', '15.5 / 2.5')")
                    }
                },
                required = listOf("expression")
            )
        ) { arguments ->
            val expression = arguments["expression"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'expression' in arguments")

            val result = calculate(expression)
            CallToolResult(
                content = listOf(
                    TextContent(text = result)
                )
            )
        }
    }

    /**
     * Получает реальную погоду для города через OpenWeatherMap API
     */
    private suspend fun getWeather(city: String): String {
        val client = weatherClient
            ?: return "❌ Ошибка: OpenWeatherMap не настроен. Установите переменную окружения OPENWEATHER_API_KEY"

        val weather = client.getCurrentWeather(city)
            ?: return "❌ Не удалось получить погоду для города '$city'. Проверьте название города."

        return buildString {
            appendLine("🌤️ Погода в городе ${weather.name}:")
            appendLine()
            appendLine("🌡️ Температура: ${weather.main.temp}°C")
            appendLine("🌡️ Ощущается как: ${weather.main.feelsLike}°C")
            weather.weather.firstOrNull()?.let {
                appendLine("☁️ Условия: ${it.description}")
            }
            appendLine("💧 Влажность: ${weather.main.humidity}%")
            appendLine("📊 Давление: ${weather.main.pressure} гПа")
            weather.wind?.let {
                appendLine("🌬️ Скорость ветра: ${it.speed} м/с")
                it.deg?.let { deg -> appendLine("🧭 Направление ветра: ${deg}°") }
            }
            weather.clouds?.let {
                appendLine("☁️ Облачность: ${it.all}%")
            }
            weather.rain?.let {
                it.oneHour?.let { rain -> appendLine("🌧️ Дождь (1ч): $rain мм") }
                it.threeHours?.let { rain -> appendLine("🌧️ Дождь (3ч): $rain мм") }
            }
            weather.snow?.let {
                it.oneHour?.let { snow -> appendLine("❄️ Снег (1ч): $snow мм") }
                it.threeHours?.let { snow -> appendLine("❄️ Снег (3ч): $snow мм") }
            }
            weather.sys?.country?.let {
                appendLine("🌍 Страна: $it")
            }
        }.trimEnd()
    }

    /**
     * Вычисляет математическое выражение.
     * Поддерживает +, -, *, /, скобки и числа с плавающей точкой.
     */
    private fun calculate(expression: String): String {
        return try {
            val result = evaluateExpression(expression.replace(" ", ""))
            "Результат вычисления '$expression' = $result"
        } catch (e: Exception) {
            "Ошибка при вычислении выражения '$expression': ${e.message}"
        }
    }

    /**
     * Вычисляет математическое выражение с использованием алгоритма с двумя стеками.
     */
    private fun evaluateExpression(expr: String): Double {
        val values = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var i = 0

        while (i < expr.length) {
            when {
                expr[i].isWhitespace() -> i++

                expr[i].isDigit() || expr[i] == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    values.add(sb.toString().toDouble())
                }

                expr[i] == '(' -> {
                    ops.add(expr[i])
                    i++
                }

                expr[i] == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') {
                        values.add(applyOp(ops.removeLast(), values.removeLast(), values.removeLast()))
                    }
                    if (ops.isNotEmpty()) {
                        ops.removeLast() // Remove '('
                    }
                    i++
                }

                expr[i] in "+-*/" -> {
                    while (ops.isNotEmpty() && hasPrecedence(expr[i], ops.last())) {
                        values.add(applyOp(ops.removeLast(), values.removeLast(), values.removeLast()))
                    }
                    ops.add(expr[i])
                    i++
                }

                else -> throw IllegalArgumentException("Недопустимый символ: ${expr[i]}")
            }
        }

        while (ops.isNotEmpty()) {
            values.add(applyOp(ops.removeLast(), values.removeLast(), values.removeLast()))
        }

        return values.last()
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        if (op2 == '(' || op2 == ')') return false
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) return false
        return true
    }

    private fun applyOp(op: Char, b: Double, a: Double): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw ArithmeticException("Деление на ноль")
                a / b
            }
            else -> throw IllegalArgumentException("Неизвестная операция: $op")
        }
    }

    /**
     * Инструмент для получения списка задач из Яндекс.Трекера
     */
    private fun registerTrackerGetIssues() {
        registerTool(
            name = "tracker_getIssues",
            description = "Получает список задач из Яндекс.Трекера. Можно фильтровать по очереди.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("queue") {
                        put("type", "string")
                        put("description", "Ключ очереди (например: QUEUE, TEST). Необязательный параметр.")
                    }
                    putJsonObject("limit") {
                        put("type", "number")
                        put("description", "Количество задач для получения (по умолчанию 10, макс 50)")
                    }
                }
            )
        ) { arguments ->
            val client = trackerClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Яндекс.Трекер не настроен. Установите переменные окружения YANDEX_TRACKER_ORG_ID и YANDEX_TRACKER_TOKEN")
                    )
                )

            val queue = arguments["queue"]?.jsonPrimitive?.content
            val limit = arguments["limit"]?.jsonPrimitive?.int ?: 10

            val issues = client.getIssues(queue = queue, limit = limit.coerceIn(1, 50))

            val resultText = if (issues.isEmpty()) {
                "Задачи не найдены"
            } else {
                buildString {
                    appendLine("Найдено задач: ${issues.size}")
                    appendLine()
                    issues.forEach { issue ->
                        appendLine("🔸 ${issue.key}: ${issue.summary}")
                        issue.status?.let { appendLine("   Статус: ${it.display}") }
                        issue.assignee?.let { appendLine("   Исполнитель: ${it.display}") }
                        issue.priority?.let { appendLine("   Приоритет: ${it.display}") }
                        appendLine()
                    }
                }
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = resultText)
                )
            )
        }
    }

    /**
     * Инструмент для создания новой задачи в Яндекс.Трекере
     */
    private fun registerTrackerCreateIssue() {
        registerTool(
            name = "tracker_createIssue",
            description = "Создает новую задачу в Яндекс.Трекере",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("queue") {
                        put("type", "string")
                        put("description", "Ключ очереди, в которой будет создана задача (например: QUEUE, TEST)")
                    }
                    putJsonObject("summary") {
                        put("type", "string")
                        put("description", "Название задачи (краткое описание)")
                    }
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Подробное описание задачи. Необязательный параметр.")
                    }
                    putJsonObject("type") {
                        put("type", "string")
                        put("description", "Тип задачи: task, bug, epic и т.д. По умолчанию: task")
                    }
                    putJsonObject("priority") {
                        put("type", "string")
                        put("description", "Приоритет: minor, normal, major, critical, blocker")
                    }
                },
                required = listOf("queue", "summary")
            )
        ) { arguments ->
            val client = trackerClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Яндекс.Трекер не настроен. Установите переменные окружения YANDEX_TRACKER_ORG_ID и YANDEX_TRACKER_TOKEN")
                    )
                )

            val queue = arguments["queue"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указана очередь"))
                )

            val summary = arguments["summary"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указано название задачи"))
                )

            val description = arguments["description"]?.jsonPrimitive?.content
            val type = arguments["type"]?.jsonPrimitive?.content ?: "task"
            val priority = arguments["priority"]?.jsonPrimitive?.content

            val issue = client.createIssue(
                queue = queue,
                summary = summary,
                description = description,
                type = type,
                priority = priority
            )

            val resultText = if (issue != null) {
                buildString {
                    appendLine("✅ Задача успешно создана!")
                    appendLine()
                    appendLine("Ключ: ${issue.key}")
                    appendLine("Название: ${issue.summary}")
                    issue.description?.let { appendLine("Описание: $it") }
                    issue.status?.let { appendLine("Статус: ${it.display}") }
                    issue.type?.let { appendLine("Тип: ${it.display}") }
                    issue.priority?.let { appendLine("Приоритет: ${it.display}") }
                }
            } else {
                "❌ Ошибка при создании задачи. Проверьте параметры и права доступа."
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = resultText)
                )
            )
        }
    }

    /**
     * Инструмент для получения информации о конкретной задаче
     */
    private fun registerTrackerGetIssue() {
        registerTool(
            name = "tracker_getIssue",
            description = "Получает подробную информацию о задаче из Яндекс.Трекера по её ключу",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("issueKey") {
                        put("type", "string")
                        put("description", "Ключ задачи (например: QUEUE-123, TEST-42)")
                    }
                },
                required = listOf("issueKey")
            )
        ) { arguments ->
            val client = trackerClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Яндекс.Трекер не настроен. Установите переменные окружения YANDEX_TRACKER_ORG_ID и YANDEX_TRACKER_TOKEN")
                    )
                )

            val issueKey = arguments["issueKey"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан ключ задачи"))
                )

            val issue = client.getIssue(issueKey)

            val resultText = if (issue != null) {
                buildString {
                    appendLine("📋 Задача: ${issue.key}")
                    appendLine()
                    appendLine("Название: ${issue.summary}")
                    issue.description?.let {
                        appendLine()
                        appendLine("Описание:")
                        appendLine(it)
                    }
                    appendLine()
                    issue.status?.let { appendLine("Статус: ${it.display}") }
                    issue.type?.let { appendLine("Тип: ${it.display}") }
                    issue.priority?.let { appendLine("Приоритет: ${it.display}") }
                    issue.assignee?.let { appendLine("Исполнитель: ${it.display}") }
                    issue.createdBy?.let { appendLine("Автор: ${it.display}") }
                    issue.createdAt?.let { appendLine("Создана: $it") }
                    issue.updatedAt?.let { appendLine("Обновлена: $it") }
                }
            } else {
                "❌ Задача с ключом '$issueKey' не найдена или нет доступа"
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = resultText)
                )
            )
        }
    }

    /**
     * Возвращает экземпляр сервера для подключения транспорта
     */
    fun getServer(): Server = server
}