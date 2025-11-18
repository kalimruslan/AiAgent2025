package ru.llm.agent

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.json.*

/**
 * MCP Сервер на основе официального Kotlin SDK.
 * Предоставляет набор инструментов для взаимодействия с AI агентами.
 */
class McpServerSdk {

    private val server: Server

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
     * Регистрирует все доступные инструменты на сервере
     */
    private fun registerTools() {
        registerEchoTool()
        registerAddTool()
        registerGetCurrentTimeTool()
        registerGetWeatherTool()
        registerCalculateTool()
    }

    /**
     * Инструмент для возврата введенного текста
     */
    private fun registerEchoTool() {
        server.addTool(
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
        ) { request ->
            val text = request.arguments["text"]?.jsonPrimitive?.content ?: ""
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
        server.addTool(
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
        ) { request ->
            val text = request.arguments["text"]?.jsonPrimitive?.content
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
        server.addTool(
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
        server.addTool(
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
        ) { request ->
            val city = request.arguments["city"]?.jsonPrimitive?.content
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
        server.addTool(
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
        ) { request ->
            val expression = request.arguments["expression"]?.jsonPrimitive?.content
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
     * Симулирует получение погоды для города.
     * В реальном приложении здесь был бы вызов к API погоды.
     */
    private fun getWeather(city: String): String {
        val weatherData = mapOf(
            "москва" to WeatherInfo(temperature = -5, condition = "Облачно", humidity = 80, windSpeed = 15),
            "moscow" to WeatherInfo(temperature = -5, condition = "Облачно", humidity = 80, windSpeed = 15),
            "санкт-петербург" to WeatherInfo(temperature = -3, condition = "Снег", humidity = 85, windSpeed = 20),
            "saint petersburg" to WeatherInfo(temperature = -3, condition = "Снег", humidity = 85, windSpeed = 20),
            "казань" to WeatherInfo(temperature = -8, condition = "Ясно", humidity = 70, windSpeed = 10),
            "kazan" to WeatherInfo(temperature = -8, condition = "Ясно", humidity = 70, windSpeed = 10),
            "новосибирск" to WeatherInfo(temperature = -15, condition = "Снег", humidity = 75, windSpeed = 25),
            "novosibirsk" to WeatherInfo(temperature = -15, condition = "Снег", humidity = 75, windSpeed = 25),
            "екатеринбург" to WeatherInfo(temperature = -10, condition = "Облачно", humidity = 78, windSpeed = 18),
            "yekaterinburg" to WeatherInfo(temperature = -10, condition = "Облачно", humidity = 78, windSpeed = 18),
            "london" to WeatherInfo(temperature = 8, condition = "Дождь", humidity = 90, windSpeed = 12),
            "paris" to WeatherInfo(temperature = 10, condition = "Облачно", humidity = 75, windSpeed = 8),
            "new york" to WeatherInfo(temperature = 5, condition = "Ясно", humidity = 60, windSpeed = 15),
            "tokyo" to WeatherInfo(temperature = 12, condition = "Ясно", humidity = 55, windSpeed = 10)
        )

        val normalizedCity = city.trim().lowercase()
        val weather = weatherData[normalizedCity]
            ?: WeatherInfo(
                temperature = (10..25).random(),
                condition = listOf("Ясно", "Облачно", "Дождь", "Снег").random(),
                humidity = (50..90).random(),
                windSpeed = (5..20).random()
            )

        return """
            Погода в городе $city:
            🌡️ Температура: ${weather.temperature}°C
            ☁️ Условия: ${weather.condition}
            💧 Влажность: ${weather.humidity}%
            🌬️ Скорость ветра: ${weather.windSpeed} км/ч
        """.trimIndent()
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
     * Возвращает экземпляр сервера для подключения транспорта
     */
    fun getServer(): Server = server

    private data class WeatherInfo(
        val temperature: Int,
        val condition: String,
        val humidity: Int,
        val windSpeed: Int
    )
}