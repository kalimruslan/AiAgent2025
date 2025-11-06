package ru.llm.agent.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.llm.agent.data.jsonrpc.JsonRpcRequest
import ru.llm.agent.data.jsonrpc.JsonRpcResponse
import ru.llm.agent.mcpmodels.CallToolResult
import ru.llm.agent.mcpmodels.Capabilities
import ru.llm.agent.mcpmodels.Content
import ru.llm.agent.mcpmodels.InitializeResult
import ru.llm.agent.mcpmodels.ServerInfo
import ru.llm.agent.mcpmodels.McpServerTool
import ru.llm.agent.mcpmodels.ToolsCapability
import ru.llm.agent.mcpmodels.McpServerToolsList
import kotlin.text.get

class McpServer {
    private val mcpServerTools = listOf(
        McpServerTool(
            name = "echo",
            description = "Возвращает введенный текст",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Текст для echo")
                    }
                }
                putJsonArray("required") {
                    add("text")
                }
            }
        ),
        McpServerTool(
            name = "add",
            description = "Складывает два числа",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "Введите выражение")
                    }
                }
                putJsonArray("required") {
                    add("text")
                }
            }
        ),
        McpServerTool(
            name = "getCurrentTime",
            description = "Возвращает текущее время",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {}
            }
        ),
        McpServerTool(
            name = "getWeather",
            description = "Получает информацию о погоде для указанного города",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("city") {
                        put("type", "string")
                        put("description", "Название города для получения погоды")
                    }
                }
                putJsonArray("required") {
                    add("city")
                }
            }
        ),
        McpServerTool(
            name = "calculate",
            description = "Вычисляет математическое выражение. Поддерживает +, -, *, /, скобки и числа с плавающей точкой",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("expression") {
                        put("type", "string")
                        put("description", "Математическое выражение для вычисления (например: '2 + 2', '(10 - 5) * 3', '15.5 / 2.5')")
                    }
                }
                putJsonArray("required") {
                    add("expression")
                }
            }
        )
    )

    fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return try {
            val result = when (request.method) {
                "initialize" -> handleInitialize()
                "tools/list" -> handleToolsList()
                "tools/call" -> handleToolsCall(request.params)
                else -> return createErrorResponse(
                    request.id,
                    -32601,
                    "Method not found: ${request.method}"
                )
            }
            JsonRpcResponse(id = request.id, result = result)
        } catch (e: Exception) {
            createErrorResponse(request.id, -32603, "Internal error: ${e.message}")
        }
    }

    private fun handleInitialize(): JsonElement {
        val result = InitializeResult(
            serverInfo = ServerInfo(
                name = "Simple MCP Server",
                version = "1.0.0"
            ),
            capabilities = Capabilities(
                tools = ToolsCapability(listChanged = false)
            )
        )
        return Json.encodeToJsonElement(result)
    }

    private fun handleToolsList(): JsonElement {
        val result = McpServerToolsList(tools = mcpServerTools)
        return Json.encodeToJsonElement(result)
    }

    private fun handleToolsCall(params: JsonObject?): JsonElement {
        if (params == null) {
            throw IllegalArgumentException("Missing params")
        }

        val toolName = params["name"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing tool name")

        val arguments = params["arguments"]?.jsonObject
            ?: JsonObject(emptyMap())

        val resultText = when (toolName) {
            "echo" -> {
                val text = arguments["text"]?.jsonPrimitive?.content ?: ""
                "Echo: $text"
            }
            "add" -> {
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

                "Result: ${a + b}"
            }
            "getCurrentTime" -> {
                "Current time: ${System.currentTimeMillis()}"
            }
            "getWeather" -> {
                val city = arguments["city"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing 'city' in arguments")
                getWeather(city)
            }
            "calculate" -> {
                val expression = arguments["expression"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing 'expression' in arguments")
                calculate(expression)
            }
            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }

        val result = CallToolResult(
            content = listOf(Content(text = resultText))
        )
        return Json.encodeToJsonElement(result)
    }

    private fun createErrorResponse(id: String, code: Int, message: String): JsonRpcResponse {
        return JsonRpcResponse(
            id = id,
            error = JsonRpcResponse.JsonRpcError(code = code, message = message)
        )
    }

    /**
     * Симулирует получение погоды для города.
     * В реальном приложении здесь был бы вызов к API погоды.
     */
    private fun getWeather(city: String): String {
        // Симулируем данные погоды для демонстрации
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

    private data class WeatherInfo(
        val temperature: Int,
        val condition: String,
        val humidity: Int,
        val windSpeed: Int
    )
}