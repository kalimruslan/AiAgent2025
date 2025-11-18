package ru.llm.agent.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Клиент для работы с Яндекс.Трекер API.
 * Документация: https://cloud.yandex.ru/docs/tracker/concepts/issues/get-issues
 */
class YandexTrackerClient(
    private val orgId: String,
    private val oauthToken: String
) {
    private val baseUrl = "https://api.tracker.yandex.net/v2"

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
     * Получает список задач из очереди
     */
    suspend fun getIssues(
        queue: String? = null,
        filter: String? = null,
        limit: Int = 10
    ): List<TrackerIssue> {
        return try {
            val response = client.get("$baseUrl/issues") {
                header("Authorization", "OAuth $oauthToken")
                header("X-Org-Id", orgId)

                parameter("perPage", limit)
                queue?.let { parameter("queue", it) }
                filter?.let { parameter("filter", it) }
            }

            response.body<List<TrackerIssue>>()
        } catch (e: Exception) {
            println("Ошибка при получении задач: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Создает новую задачу
     */
    suspend fun createIssue(
        queue: String,
        summary: String,
        description: String? = null,
        type: String = "task",
        priority: String? = null
    ): TrackerIssue? {
        return try {
            val requestBody = CreateIssueRequest(
                queue = queue,
                summary = summary,
                description = description,
                type = type,
                priority = priority
            )

            val response = client.post("$baseUrl/issues") {
                header("Authorization", "OAuth $oauthToken")
                header("X-Org-Id", orgId)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            response.body<TrackerIssue>()
        } catch (e: Exception) {
            println("Ошибка при создании задачи: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает информацию о задаче по ключу
     */
    suspend fun getIssue(issueKey: String): TrackerIssue? {
        return try {
            val response = client.get("$baseUrl/issues/$issueKey") {
                header("Authorization", "OAuth $oauthToken")
                header("X-Org-Id", orgId)
            }

            response.body<TrackerIssue>()
        } catch (e: Exception) {
            println("Ошибка при получении задачи $issueKey: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun close() {
        client.close()
    }

    @Serializable
    data class TrackerIssue(
        val key: String,
        val summary: String,
        val description: String? = null,
        val status: Status? = null,
        val type: Type? = null,
        val priority: Priority? = null,
        val assignee: User? = null,
        val createdBy: User? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null
    )

    @Serializable
    data class Status(
        val key: String,
        val display: String
    )

    @Serializable
    data class Type(
        val key: String,
        val display: String
    )

    @Serializable
    data class Priority(
        val key: String,
        val display: String
    )

    @Serializable
    data class User(
        val id: String,
        val display: String
    )

    @Serializable
    private data class CreateIssueRequest(
        val queue: String,
        val summary: String,
        val description: String? = null,
        val type: String? = null,
        val priority: String? = null
    )
}