package ru.llm.agent.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import ru.llm.agent.database.MessageDatabase
import ru.llm.agent.database.mcpserver.McpServerEntity
import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.model.mcp.McpServerType

/**
 * Реализация репозитория для управления удаленными MCP серверами
 */
public class McpServerRepositoryImpl(
    private val database: MessageDatabase
) : McpServerRepository {

    override fun getAllServers(): Flow<List<McpServer>> {
        return database.mcpServerReadDao().getAllServers()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActiveServers(): Flow<List<McpServer>> {
        return database.mcpServerReadDao().getActiveServers()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getServerById(id: Long): McpServer? {
        return database.mcpServerReadDao().getServerById(id)?.toDomain()
    }

    override suspend fun addServer(server: McpServer): Long {
        return database.mcpServerWriteDao().insertServer(server.toEntity())
    }

    override suspend fun updateServer(server: McpServer) {
        database.mcpServerWriteDao().updateServer(server.toEntity())
    }

    override suspend fun deleteServer(id: Long) {
        database.mcpServerWriteDao().deleteServer(id)
    }

    override suspend fun setServerActive(id: Long, isActive: Boolean) {
        database.mcpServerWriteDao().setServerActive(id, isActive)
    }

    private fun McpServerEntity.toDomain(): McpServer {
        return McpServer(
            id = id,
            name = name,
            type = McpServerType.valueOf(type),
            url = url,
            command = command,
            args = args?.let { Json.decodeFromString<List<String>>(it) },
            env = env?.let { Json.decodeFromString<Map<String, String>>(it) },
            description = description,
            isActive = isActive,
            timestamp = timestamp
        )
    }

    private fun McpServer.toEntity(): McpServerEntity {
        return McpServerEntity(
            id = id,
            name = name,
            type = type.name,
            url = url,
            command = command,
            args = args?.let { Json.encodeToString(it) },
            env = env?.let { Json.encodeToString(it) },
            description = description,
            isActive = isActive,
            timestamp = timestamp
        )
    }
}