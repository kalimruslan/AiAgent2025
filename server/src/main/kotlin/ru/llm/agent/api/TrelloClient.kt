package ru.llm.agent.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Клиент для работы с Trello REST API.
 * Документация: https://developer.atlassian.com/cloud/trello/rest/
 */
class TrelloClient(
    private val apiKey: String,
    private val token: String
) {
    private val baseUrl = "https://api.trello.com/1"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Получает список карточек с доски
     */
    suspend fun getCards(
        boardId: String,
        filter: String = "open",
        limit: Int? = null
    ): List<TrelloCard> {
        return try {
            val response = client.get("$baseUrl/boards/$boardId/cards") {
                parameter("key", apiKey)
                parameter("token", token)
                parameter("filter", filter)
                limit?.let { parameter("limit", it) }
            }

            response.body<List<TrelloCard>>()
        } catch (e: Exception) {
            println("Ошибка при получении карточек: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Создает новую карточку
     */
    suspend fun createCard(
        idList: String,
        name: String,
        desc: String? = null,
        due: String? = null,
        pos: String = "bottom"
    ): TrelloCard? {
        return try {
            val response = client.post("$baseUrl/cards") {
                parameter("key", apiKey)
                parameter("token", token)
                parameter("idList", idList)
                parameter("name", name)
                desc?.let { parameter("desc", it) }
                due?.let { parameter("due", it) }
                parameter("pos", pos)
            }

            response.body<TrelloCard>()
        } catch (e: Exception) {
            println("Ошибка при создании карточки: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает информацию о карточке по ID
     */
    suspend fun getCard(cardId: String): TrelloCard? {
        return try {
            val response = client.get("$baseUrl/cards/$cardId") {
                parameter("key", apiKey)
                parameter("token", token)
            }

            response.body<TrelloCard>()
        } catch (e: Exception) {
            println("Ошибка при получении карточки $cardId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает списки (колонки) на доске
     */
    suspend fun getLists(boardId: String): List<TrelloList> {
        return try {
            val response = client.get("$baseUrl/boards/$boardId/lists") {
                parameter("key", apiKey)
                parameter("token", token)
            }

            response.body<List<TrelloList>>()
        } catch (e: Exception) {
            println("Ошибка при получении списков: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Получает статистику по доске
     */
    suspend fun getBoardSummary(boardId: String): BoardSummary {
        val cards = getCards(boardId, filter = "open")
        val lists = getLists(boardId)

        val today = LocalDate.now()

        // Карточки с дедлайном на сегодня
        val dueToday = cards.filter { card ->
            card.due?.let { dueDate ->
                try {
                    val cardDate = Instant.parse(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    cardDate == today
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }

        val dueTodayCompleted = dueToday.count { it.dueComplete == true }

        // Просроченные карточки
        val overdue = cards.filter { card ->
            card.due?.let { dueDate ->
                try {
                    val cardDate = Instant.parse(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    cardDate.isBefore(today) && card.dueComplete != true
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }

        // Активность за сегодня
        val updatedToday = cards.count { card ->
            card.dateLastActivity?.let { lastActivity ->
                try {
                    val activityDate = Instant.parse(lastActivity)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    activityDate == today
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }

        // Группировка по спискам
        val cardsByList = cards.groupBy { it.idList }
            .mapValues { it.value.size }

        // Создаем мапу ID списка -> название
        val listNames = lists.associate { it.id to it.name }

        return BoardSummary(
            totalCards = cards.size,
            dueTodayTotal = dueToday.size,
            dueTodayCompleted = dueTodayCompleted,
            overdueCount = overdue.size,
            updatedTodayCount = updatedToday,
            cardsByList = cardsByList,
            listNames = listNames
        )
    }

    /**
     * Обновляет карточку (перемещение в другой список, изменение названия, описания, дедлайна)
     */
    suspend fun updateCard(
        cardId: String,
        name: String? = null,
        desc: String? = null,
        idList: String? = null,
        due: String? = null,
        dueComplete: Boolean? = null
    ): TrelloCard? {
        return try {
            val response = client.put("$baseUrl/cards/$cardId") {
                parameter("key", apiKey)
                parameter("token", token)
                name?.let { parameter("name", it) }
                desc?.let { parameter("desc", it) }
                idList?.let { parameter("idList", it) }
                due?.let { parameter("due", it) }
                dueComplete?.let { parameter("dueComplete", it) }
            }

            response.body<TrelloCard>()
        } catch (e: Exception) {
            println("Ошибка при обновлении карточки $cardId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Поиск карточек на доске по различным критериям
     */
    suspend fun searchCards(
        boardId: String,
        query: String? = null,
        dueFilter: String? = null,
        labels: List<String>? = null
    ): List<TrelloCard> {
        return try {
            val allCards = getCards(boardId, filter = "open")

            allCards.filter { card ->
                var matches = true

                // Фильтр по текстовому запросу (в названии или описании)
                if (!query.isNullOrBlank()) {
                    matches = matches && (
                        card.name.contains(query, ignoreCase = true) ||
                        card.desc?.contains(query, ignoreCase = true) == true
                    )
                }

                // Фильтр по дедлайнам
                if (!dueFilter.isNullOrBlank()) {
                    matches = matches && when (dueFilter) {
                        "today" -> card.due?.let { dueDate ->
                            try {
                                val cardDate = Instant.parse(dueDate)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                cardDate == LocalDate.now()
                            } catch (e: Exception) {
                                false
                            }
                        } ?: false

                        "overdue" -> card.due?.let { dueDate ->
                            try {
                                val cardDate = Instant.parse(dueDate)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                cardDate.isBefore(LocalDate.now()) && card.dueComplete != true
                            } catch (e: Exception) {
                                false
                            }
                        } ?: false

                        "week" -> card.due?.let { dueDate ->
                            try {
                                val cardDate = Instant.parse(dueDate)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val weekLater = LocalDate.now().plusWeeks(1)
                                cardDate.isAfter(LocalDate.now()) && cardDate.isBefore(weekLater)
                            } catch (e: Exception) {
                                false
                            }
                        } ?: false

                        else -> true
                    }
                }

                // Фильтр по меткам
                if (!labels.isNullOrEmpty()) {
                    matches = matches && card.labels?.any { label ->
                        labels.any { searchLabel ->
                            label.name?.contains(searchLabel, ignoreCase = true) == true ||
                            label.color?.equals(searchLabel, ignoreCase = true) == true
                        }
                    } ?: false
                }

                matches
            }
        } catch (e: Exception) {
            println("Ошибка при поиске карточек: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun close() {
        client.close()
    }

    /**
     * Модель данных карточки Trello
     */
    @Serializable
    data class TrelloCard(
        val id: String,
        val name: String,
        val desc: String? = null,
        val idList: String,
        val idBoard: String,
        val due: String? = null,
        val dueComplete: Boolean? = null,
        val dateLastActivity: String? = null,
        val labels: List<Label>? = null,
        val closed: Boolean = false,
        val url: String? = null,
        val shortUrl: String? = null
    )

    /**
     * Модель данных списка (колонки) Trello
     */
    @Serializable
    data class TrelloList(
        val id: String,
        val name: String,
        val closed: Boolean = false,
        val pos: Double? = null,
        val idBoard: String
    )

    /**
     * Модель данных метки
     */
    @Serializable
    data class Label(
        val id: String,
        val name: String? = null,
        val color: String? = null
    )

    /**
     * Статистика по доске
     */
    data class BoardSummary(
        val totalCards: Int,
        val dueTodayTotal: Int,
        val dueTodayCompleted: Int,
        val overdueCount: Int,
        val updatedTodayCount: Int,
        val cardsByList: Map<String, Int>,
        val listNames: Map<String, String>
    )
}