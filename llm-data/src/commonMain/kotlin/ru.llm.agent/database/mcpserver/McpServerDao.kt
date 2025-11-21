package ru.llm.agent.database.mcpserver

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO для чтения MCP серверов
 */
@Dao
public interface McpServerReadDao {
    /**
     * Получить все серверы
     */
    @Query("SELECT * FROM mcp_servers ORDER BY timestamp DESC")
    public fun getAllServers(): Flow<List<McpServerEntity>>

    /**
     * Получить активные серверы
     */
    @Query("SELECT * FROM mcp_servers WHERE isActive = 1 ORDER BY timestamp DESC")
    public fun getActiveServers(): Flow<List<McpServerEntity>>

    /**
     * Получить сервер по ID
     */
    @Query("SELECT * FROM mcp_servers WHERE id = :id")
    public suspend fun getServerById(id: Long): McpServerEntity?
}

/**
 * DAO для записи MCP серверов
 */
@Dao
public interface McpServerWriteDao {
    /**
     * Добавить новый сервер
     */
    @Insert
    public suspend fun insertServer(server: McpServerEntity): Long

    /**
     * Обновить сервер
     */
    @Update
    public suspend fun updateServer(server: McpServerEntity)

    /**
     * Удалить сервер
     */
    @Query("DELETE FROM mcp_servers WHERE id = :id")
    public suspend fun deleteServer(id: Long)

    /**
     * Переключить активность сервера
     */
    @Query("UPDATE mcp_servers SET isActive = :isActive WHERE id = :id")
    public suspend fun setServerActive(id: Long, isActive: Boolean)
}

/**
 * Объединенный DAO для обратной совместимости
 */
@Dao
public interface McpServerDao : McpServerReadDao, McpServerWriteDao