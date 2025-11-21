package ru.llm.agent

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.data.mcp.McpTool
import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.repository.McpServerRepository
import kotlinx.serialization.json.JsonObject

/**
 * Менеджер для работы с несколькими удаленными MCP клиентами.
 * Управляет подключениями ко всем активным удаленным MCP серверам.
 */
public class McpClientsManager(
    private val mcpServerRepository: McpServerRepository,
    private val httpClient: HttpClient
) {
    private val logger: Logger = createLogger("McpClientsManager")

    // Кэш клиентов для удаленных серверов
    private val remoteClients = mutableMapOf<Long, McpClient>()

    /**
     * Получить все инструменты со всех активных удаленных серверов
     */
    public suspend fun getAllTools(): Flow<List<McpTool>> {
        return mcpServerRepository.getActiveServers().map { activeServers ->
            val allTools = mutableListOf<McpTool>()

            // Получаем инструменты со всех активных удаленных серверов
            activeServers.forEach { server ->
                try {
                    val client = getOrCreateClient(server)
                    val remoteTools = client.listTools()
                    allTools.addAll(remoteTools)
                    logger.info("Loaded ${remoteTools.size} tools from remote server: ${server.name}")
                } catch (e: Exception) {
                    logger.warning("Failed to load tools from ${server.name}: ${e.message}")
                }
            }

            allTools
        }
    }

    /**
     * Вызвать инструмент на соответствующем удаленном сервере
     */
    public suspend fun callTool(name: String, arguments: JsonObject): String {
        // Получаем список активных серверов (используем first() вместо collect)
        val activeServers = mcpServerRepository.getActiveServers().first()
        logger.info("Active servers - $activeServers")

        // Пробуем вызвать инструмент на каждом сервере
        for (server in activeServers) {
            try {
                logger.info("Choose server - ${server} in active servers $activeServers")
                val client = getOrCreateClient(server)
                logger.info("Call tool $name on ${server.name}, has client: $client")
                return client.callTool(name, arguments)
            } catch (e: Exception) {
                logger.info("Tool $name not found on ${server.name}, trying next server...")
            }
        }

        throw IllegalArgumentException("Tool $name not found on any active MCP server")
    }

    /**
     * Получить или создать клиента для удаленного сервера
     */
    private fun getOrCreateClient(server: McpServer): McpClient {
        return remoteClients.getOrPut(server.id) {
            logger.info("Creating new MCP client for remote server: ${server.name} at ${server.url}")
            McpClient(
                serverUrl = server.url,
                client = httpClient
            )
        }
    }

    /**
     * Очистить кэш клиентов (например, при изменении списка серверов)
     */
    public fun clearCache() {
        remoteClients.clear()
        logger.info("Cleared MCP clients cache")
    }
}