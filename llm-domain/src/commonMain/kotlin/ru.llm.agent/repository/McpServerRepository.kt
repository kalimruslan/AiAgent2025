package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import ru.llm.agent.model.mcp.McpServer

/**
 * Репозиторий для управления удаленными MCP серверами
 */
public interface McpServerRepository {
    /**
     * Получить все серверы
     */
    public fun getAllServers(): Flow<List<McpServer>>

    /**
     * Получить активные серверы
     */
    public fun getActiveServers(): Flow<List<McpServer>>

    /**
     * Получить сервер по ID
     */
    public suspend fun getServerById(id: Long): McpServer?

    /**
     * Добавить новый сервер
     */
    public suspend fun addServer(server: McpServer): Long

    /**
     * Обновить сервер
     */
    public suspend fun updateServer(server: McpServer)

    /**
     * Удалить сервер
     */
    public suspend fun deleteServer(id: Long)

    /**
     * Переключить активность сервера
     */
    public suspend fun setServerActive(id: Long, isActive: Boolean)
}