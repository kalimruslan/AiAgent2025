package ru.llm.agent

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.json.*
import ru.llm.agent.api.TrelloClient
import ru.llm.agent.api.OpenWeatherMapClient
import java.time.LocalDate

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
     * Клиент для работы с Trello (lazy initialization)
     */
    private val trelloClient: TrelloClient? by lazy {
        val apiKey = System.getenv("TRELLO_API_KEY")
        val token = System.getenv("TRELLO_TOKEN")

        if (apiKey != null && token != null) {
            TrelloClient(apiKey, token)
        } else {
            println("WARN: Trello не настроен. Установите TRELLO_API_KEY и TRELLO_TOKEN")
            null
        }
    }

    /**
     * Клиент для работы с OpenWeatherMap (lazy initialization)
     */
    private val weatherClient: OpenWeatherMapClient? by lazy {
        val apiKey = System.getenv("OPENWEATHER_API_KEY")
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
        registerGetWeatherTool()
        // Trello инструменты
        registerTrelloGetCards()
        registerTrelloCreateCard()
        registerTrelloGetCard()
        registerTrelloGetSummary()
        // Композитные Trello инструменты
        registerTrelloQuickTask()
        registerTrelloMoveCard()
        registerTrelloUpdateCard()
        registerTrelloSearchCards()
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
     * Инструмент для получения списка карточек из Trello
     */
    private fun registerTrelloGetCards() {
        registerTool(
            name = "trello_getCards",
            description = "Получает список карточек с доски Trello. Можно фильтровать по статусу.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("boardId") {
                        put("type", "string")
                        put("description", "ID доски Trello (обязательный параметр)")
                    }
                    putJsonObject("filter") {
                        put("type", "string")
                        put("description", "Фильтр: open (активные), closed (архивные), all (все). По умолчанию: open")
                    }
                    putJsonObject("limit") {
                        put("type", "number")
                        put("description", "Количество карточек для получения")
                    }
                },
                required = listOf("boardId")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Trello не настроен. Установите переменные окружения TRELLO_API_KEY и TRELLO_TOKEN")
                    )
                )

            val boardId = arguments["boardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан ID доски"))
                )

            val filter = arguments["filter"]?.jsonPrimitive?.content ?: "open"
            val limit = arguments["limit"]?.jsonPrimitive?.int

            val cards = client.getCards(boardId = boardId, filter = filter, limit = limit)

            val resultText = if (cards.isEmpty()) {
                "Карточки не найдены"
            } else {
                buildString {
                    appendLine("Найдено карточек: ${cards.size}")
                    appendLine()
                    cards.forEach { card ->
                        appendLine("🔹 ${card.name}")
                        card.desc?.takeIf { it.isNotEmpty() }?.let { appendLine("   Описание: $it") }
                        card.due?.let { appendLine("   Дедлайн: $it") }
                        card.dueComplete?.let { appendLine("   Выполнено: ${if (it) "✅" else "⏳"}") }
                        card.labels?.takeIf { it.isNotEmpty() }?.let { labels ->
                            appendLine("   Метки: ${labels.joinToString { it.name ?: it.color ?: "?" }}")
                        }
                        card.url?.let { appendLine("   URL: $it") }
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
     * Инструмент для создания новой карточки в Trello
     */
    private fun registerTrelloCreateCard() {
        registerTool(
            name = "trello_createCard",
            description = "Создает новую карточку в Trello",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("idList") {
                        put("type", "string")
                        put("description", "ID списка (колонки), в который будет добавлена карточка")
                    }
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Название карточки")
                    }
                    putJsonObject("desc") {
                        put("type", "string")
                        put("description", "Описание карточки. Необязательный параметр.")
                    }
                    putJsonObject("due") {
                        put("type", "string")
                        put("description", "Дедлайн в формате ISO 8601 (например: 2025-11-20T12:00:00Z)")
                    }
                },
                required = listOf("idList", "name")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Trello не настроен. Установите переменные окружения TRELLO_API_KEY и TRELLO_TOKEN")
                    )
                )

            val idList = arguments["idList"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан ID списка"))
                )

            val name = arguments["name"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указано название карточки"))
                )

            val desc = arguments["desc"]?.jsonPrimitive?.content
            val due = arguments["due"]?.jsonPrimitive?.content

            val card = client.createCard(
                idList = idList,
                name = name,
                desc = desc,
                due = due
            )

            val resultText = if (card != null) {
                buildString {
                    appendLine("✅ Карточка успешно создана!")
                    appendLine()
                    appendLine("Название: ${card.name}")
                    card.desc?.let { appendLine("Описание: $it") }
                    card.due?.let { appendLine("Дедлайн: $it") }
                    card.url?.let { appendLine("URL: $it") }
                    card.shortUrl?.let { appendLine("Короткая ссылка: $it") }
                }
            } else {
                "❌ Ошибка при создании карточки. Проверьте параметры и права доступа."
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = resultText)
                )
            )
        }
    }

    /**
     * Инструмент для получения информации о конкретной карточке
     */
    private fun registerTrelloGetCard() {
        registerTool(
            name = "trello_getCard",
            description = "Получает подробную информацию о карточке Trello по её ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("cardId") {
                        put("type", "string")
                        put("description", "ID карточки Trello")
                    }
                },
                required = listOf("cardId")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Trello не настроен. Установите переменные окружения TRELLO_API_KEY и TRELLO_TOKEN")
                    )
                )

            val cardId = arguments["cardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан ID карточки"))
                )

            val card = client.getCard(cardId)

            val resultText = if (card != null) {
                buildString {
                    appendLine("📋 Карточка: ${card.name}")
                    appendLine()
                    card.desc?.takeIf { it.isNotEmpty() }?.let {
                        appendLine("Описание:")
                        appendLine(it)
                        appendLine()
                    }
                    card.due?.let { appendLine("Дедлайн: $it") }
                    card.dueComplete?.let { appendLine("Статус выполнения: ${if (it) "✅ Завершено" else "⏳ В процессе"}") }
                    card.labels?.takeIf { it.isNotEmpty() }?.let { labels ->
                        appendLine("Метки: ${labels.joinToString { it.name ?: it.color ?: "?" }}")
                    }
                    card.dateLastActivity?.let { appendLine("Последняя активность: $it") }
                    card.url?.let { appendLine("URL: $it") }
                    appendLine("Закрыта: ${if (card.closed) "Да" else "Нет"}")
                }
            } else {
                "❌ Карточка с ID '$cardId' не найдена или нет доступа"
            }

            CallToolResult(
                content = listOf(
                    TextContent(text = resultText)
                )
            )
        }
    }

    /**
     * Инструмент для получения статистики по доске Trello
     */
    private fun registerTrelloGetSummary() {
        registerTool(
            name = "trello_getSummary",
            description = "Получает статистику по доске Trello: карточки на сегодня, выполненные, просроченные, активность",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("boardId") {
                        put("type", "string")
                        put("description", "ID доски Trello")
                    }
                },
                required = listOf("boardId")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Trello не настроен. Установите переменные окружения TRELLO_API_KEY и TRELLO_TOKEN")
                    )
                )

            val boardId = arguments["boardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан ID доски"))
                )

            val summary = client.getBoardSummary(boardId)

            val resultText = buildString {
                appendLine("📊 Статистика по доске")
                appendLine()
                appendLine("📋 Всего активных карточек: ${summary.totalCards}")
                appendLine()
                appendLine("📅 Задачи на сегодня:")
                appendLine("  • Всего: ${summary.dueTodayTotal}")
                appendLine("  • Выполнено: ${summary.dueTodayCompleted}")
                appendLine("  • Осталось: ${summary.dueTodayTotal - summary.dueTodayCompleted}")
                appendLine()
                if (summary.overdueCount > 0) {
                    appendLine("⚠️ Просрочено: ${summary.overdueCount} карточек")
                    appendLine()
                }
                appendLine("🔥 Активность за сегодня: ${summary.updatedTodayCount} карточек")
                appendLine()
                if (summary.cardsByList.isNotEmpty()) {
                    appendLine("📋 По спискам:")
                    summary.cardsByList.forEach { (listId, count) ->
                        val listName = summary.listNames[listId] ?: listId
                        appendLine("  • $listName: $count карточек")
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
     * Инструмент для быстрого создания задачи с минимумом параметров
     */
    private fun registerTrelloQuickTask() {
        registerTool(
            name = "trello_quickTask",
            description = "Быстро создает задачу в Trello с минимумом параметров. Автоматически ставит дедлайн на 'сегодня', 'завтра' или конкретную дату. Ищет первый открытый список для добавления.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("boardId") {
                        put("type", "string")
                        put("description", "ID доски Trello")
                    }
                    putJsonObject("task") {
                        put("type", "string")
                        put("description", "Название задачи")
                    }
                    putJsonObject("when") {
                        put("type", "string")
                        put("description", "Когда выполнить: 'today' (сегодня), 'tomorrow' (завтра), или дата в формате ISO 8601")
                    }
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Необязательное описание задачи")
                    }
                },
                required = listOf("boardId", "task")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка: Trello не настроен")
                    )
                )

            val boardId = arguments["boardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан boardId"))
                )

            val task = arguments["task"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указана задача"))
                )

            val whenParam = arguments["when"]?.jsonPrimitive?.content
            val description = arguments["description"]?.jsonPrimitive?.content

            // Получаем списки доски
            val lists = client.getLists(boardId)
            if (lists.isEmpty()) {
                return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: на доске нет списков"))
                )
            }

            // Берём первый открытый список
            val targetList = lists.firstOrNull { !it.closed }
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: нет открытых списков"))
                )

            // Парсим дедлайн
            val dueDate = when (whenParam?.lowercase()) {
                "today", "сегодня" -> {
                    val today = LocalDate.now()
                    "${today}T23:59:59Z"
                }
                "tomorrow", "завтра" -> {
                    val tomorrow = LocalDate.now().plusDays(1)
                    "${tomorrow}T23:59:59Z"
                }
                null -> null
                else -> whenParam // Предполагаем, что это ISO 8601 формат
            }

            val card = client.createCard(
                idList = targetList.id,
                name = task,
                desc = description,
                due = dueDate
            )

            val resultText = if (card != null) {
                buildString {
                    appendLine("✅ Быстрая задача создана!")
                    appendLine()
                    appendLine("📝 ${card.name}")
                    card.desc?.let { appendLine("💬 $it") }
                    card.due?.let { appendLine("📅 Дедлайн: $it") }
                    appendLine("📋 Список: ${targetList.name}")
                    card.shortUrl?.let { appendLine("🔗 $it") }
                }
            } else {
                "❌ Не удалось создать быструю задачу"
            }

            CallToolResult(
                content = listOf(TextContent(text = resultText))
            )
        }
    }

    /**
     * Инструмент для перемещения карточки между списками
     */
    private fun registerTrelloMoveCard() {
        registerTool(
            name = "trello_moveCard",
            description = "Перемещает карточку в другой список на доске. Можно указать название списка или его ID.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("cardId") {
                        put("type", "string")
                        put("description", "ID карточки для перемещения")
                    }
                    putJsonObject("targetList") {
                        put("type", "string")
                        put("description", "Название или ID целевого списка")
                    }
                    putJsonObject("boardId") {
                        put("type", "string")
                        put("description", "ID доски (нужен если targetList - это название)")
                    }
                },
                required = listOf("cardId", "targetList")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: Trello не настроен"))
                )

            val cardId = arguments["cardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан cardId"))
                )

            val targetList = arguments["targetList"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан targetList"))
                )

            val boardId = arguments["boardId"]?.jsonPrimitive?.content

            // Определяем, это ID или название списка
            val targetListId = if (targetList.length == 24) {
                // Скорее всего это ID Trello (24 символа)
                targetList
            } else {
                // Это название - нужно найти список
                if (boardId == null) {
                    return@registerTool CallToolResult(
                        content = listOf(TextContent(text = "Ошибка: для поиска по названию нужен boardId"))
                    )
                }

                val lists = client.getLists(boardId)
                val foundList = lists.firstOrNull {
                    it.name.equals(targetList, ignoreCase = true)
                }

                foundList?.id ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: список '$targetList' не найден"))
                )
            }

            val updatedCard = client.updateCard(
                cardId = cardId,
                idList = targetListId
            )

            val resultText = if (updatedCard != null) {
                buildString {
                    appendLine("✅ Карточка перемещена!")
                    appendLine()
                    appendLine("📝 ${updatedCard.name}")
                    appendLine("📋 Новый список: ID = ${updatedCard.idList}")
                    updatedCard.url?.let { appendLine("🔗 $it") }
                }
            } else {
                "❌ Не удалось переместить карточку"
            }

            CallToolResult(
                content = listOf(TextContent(text = resultText))
            )
        }
    }

    /**
     * Инструмент для обновления карточки
     */
    private fun registerTrelloUpdateCard() {
        registerTool(
            name = "trello_updateCard",
            description = "Обновляет существующую карточку: название, описание, дедлайн, статус выполнения",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("cardId") {
                        put("type", "string")
                        put("description", "ID карточки для обновления")
                    }
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Новое название карточки")
                    }
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Новое описание карточки")
                    }
                    putJsonObject("due") {
                        put("type", "string")
                        put("description", "Новый дедлайн в формате ISO 8601 или 'today', 'tomorrow'")
                    }
                    putJsonObject("dueComplete") {
                        put("type", "boolean")
                        put("description", "Отметить дедлайн как выполненный (true) или невыполненный (false)")
                    }
                },
                required = listOf("cardId")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: Trello не настроен"))
                )

            val cardId = arguments["cardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан cardId"))
                )

            val name = arguments["name"]?.jsonPrimitive?.content
            val description = arguments["description"]?.jsonPrimitive?.content
            val dueParam = arguments["due"]?.jsonPrimitive?.content
            val dueComplete = arguments["dueComplete"]?.jsonPrimitive?.booleanOrNull

            // Парсим дедлайн
            val due = when (dueParam?.lowercase()) {
                "today", "сегодня" -> {
                    val today = LocalDate.now()
                    "${today}T23:59:59Z"
                }
                "tomorrow", "завтра" -> {
                    val tomorrow = LocalDate.now().plusDays(1)
                    "${tomorrow}T23:59:59Z"
                }
                null -> null
                else -> dueParam
            }

            val updatedCard = client.updateCard(
                cardId = cardId,
                name = name,
                desc = description,
                due = due,
                dueComplete = dueComplete
            )

            val resultText = if (updatedCard != null) {
                buildString {
                    appendLine("✅ Карточка обновлена!")
                    appendLine()
                    appendLine("📝 ${updatedCard.name}")
                    updatedCard.desc?.let { appendLine("💬 $it") }
                    updatedCard.due?.let { appendLine("📅 Дедлайн: $it") }
                    updatedCard.dueComplete?.let {
                        appendLine("✔️ Выполнено: ${if (it) "Да" else "Нет"}")
                    }
                    updatedCard.url?.let { appendLine("🔗 $it") }
                }
            } else {
                "❌ Не удалось обновить карточку"
            }

            CallToolResult(
                content = listOf(TextContent(text = resultText))
            )
        }
    }

    /**
     * Инструмент для поиска карточек
     */
    private fun registerTrelloSearchCards() {
        registerTool(
            name = "trello_searchCards",
            description = "Поиск карточек на доске по тексту, дедлайнам, меткам. Мощный инструмент для фильтрации задач.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("boardId") {
                        put("type", "string")
                        put("description", "ID доски Trello")
                    }
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Текст для поиска в названии или описании карточки")
                    }
                    putJsonObject("dueFilter") {
                        put("type", "string")
                        put("description", "Фильтр по дедлайну: 'today' (сегодня), 'week' (неделя), 'overdue' (просрочено)")
                    }
                    putJsonObject("labels") {
                        put("type", "string")
                        put("description", "Метки для поиска (через запятую)")
                    }
                },
                required = listOf("boardId")
            )
        ) { arguments ->
            val client = trelloClient
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: Trello не настроен"))
                )

            val boardId = arguments["boardId"]?.jsonPrimitive?.content
                ?: return@registerTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка: не указан boardId"))
                )

            val query = arguments["query"]?.jsonPrimitive?.content
            val dueFilter = arguments["dueFilter"]?.jsonPrimitive?.content
            val labelsStr = arguments["labels"]?.jsonPrimitive?.content
            val labels = labelsStr?.split(",")?.map { it.trim() }

            val cards = client.searchCards(
                boardId = boardId,
                query = query,
                dueFilter = dueFilter,
                labels = labels
            )

            val resultText = if (cards.isEmpty()) {
                "🔍 Карточки не найдены по заданным критериям"
            } else {
                buildString {
                    appendLine("🔍 Найдено карточек: ${cards.size}")
                    appendLine()
                    cards.take(20).forEach { card ->
                        appendLine("🔹 ${card.name}")
                        card.desc?.takeIf { it.isNotEmpty() }?.let {
                            val preview = if (it.length > 100) it.take(100) + "..." else it
                            appendLine("   💬 $preview")
                        }
                        card.due?.let { appendLine("   📅 $it") }
                        card.labels?.takeIf { it.isNotEmpty() }?.let { labels ->
                            appendLine("   🏷️ ${labels.joinToString { it.name ?: it.color ?: "?" }}")
                        }
                        card.shortUrl?.let { appendLine("   🔗 $it") }
                        appendLine()
                    }

                    if (cards.size > 20) {
                        appendLine("... и ещё ${cards.size - 20} карточек")
                    }
                }
            }

            CallToolResult(
                content = listOf(TextContent(text = resultText))
            )
        }
    }

    /**
     * Возвращает экземпляр сервера для подключения транспорта
     */
    fun getServer(): Server = server
}