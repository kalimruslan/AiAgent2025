package ru.llm.agent

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.data.mcp.McpTool
import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.model.mcp.McpServerType
import ru.llm.agent.repository.McpServerRepository
import kotlinx.serialization.json.JsonObject

/**
 * Менеджер для работы с MCP клиентами (удаленными и локальными).
 * Управляет подключениями ко всем активным MCP серверам.
 */
public class McpClientsManager(
    private val mcpServerRepository: McpServerRepository,
    private val httpClient: HttpClient
) {
    private val logger: Logger = createLogger("McpClientsManager")

    // Кэш клиентов для удаленных серверов
    private val remoteClients = mutableMapOf<Long, McpClient>()

    // Кэш клиентов для локальных серверов
    private val stdioClients = mutableMapOf<Long, McpStdioClient>()

    /**
     * Получить все инструменты со всех активных серверов (удаленных и локальных)
     */
    public suspend fun getAllTools(): Flow<List<McpTool>> {
        return mcpServerRepository.getActiveServers().map { activeServers ->
            val allTools = mutableListOf<McpTool>()

            // Получаем инструменты со всех активных серверов
            activeServers.forEach { server ->
                try {
                    val tools = when (server.type) {
                        McpServerType.REMOTE -> {
                            val client = getOrCreateRemoteClient(server)
                            client.listTools()
                        }
                        McpServerType.LOCAL -> {
                            val client = getOrCreateStdioClient(server)
                            client.listTools()
                        }
                    }
                    allTools.addAll(tools)
                    logger.info("Loaded ${tools.size} tools from ${server.type} server: ${server.name}")
                } catch (e: Exception) {
                    logger.warning("Failed to load tools from ${server.name}: ${e.message}")
                }
            }

            allTools
        }
    }

    /**
     * Вызвать инструмент на соответствующем сервере (удаленном или локальном)
     */
    public suspend fun callTool(name: String, arguments: JsonObject): String {
        val activeServers = mcpServerRepository.getActiveServers().first()
        logger.info("Active servers - $activeServers")

        // Пробуем вызвать инструмент на каждом сервере
        for (server in activeServers) {
            try {
                logger.info("Trying to call tool $name on ${server.type} server: ${server.name}")

                val result = when (server.type) {
                    McpServerType.REMOTE -> {
                        val client = getOrCreateRemoteClient(server)
                        client.callTool(name, arguments)
                    }
                    McpServerType.LOCAL -> {
                        val client = getOrCreateStdioClient(server)
                        client.callTool(name, arguments)
                    }
                }

                logger.info("Successfully called tool $name on ${server.name}")
                return result
            } catch (e: Exception) {
                logger.info("Tool $name not found on ${server.name}, trying next server... Error: ${e.message}")
            }
        }

        throw IllegalArgumentException("Tool $name not found on any active MCP server")
    }

    /**
     * Получить или создать клиента для удаленного сервера
     */
    private fun getOrCreateRemoteClient(server: McpServer): McpClient {
        return remoteClients.getOrPut(server.id) {
            logger.info("Creating new MCP remote client for: ${server.name} at ${server.url}")
            McpClient(
                serverUrl = server.url!!,
                client = httpClient
            )
        }
    }

    /**
     * Получить или создать клиента для локального сервера через stdio
     */
    private suspend fun getOrCreateStdioClient(server: McpServer): McpStdioClient {
        return stdioClients.getOrPut(server.id) {
            logger.info("Creating new MCP stdio client for: ${server.name}, command: ${server.command}")
            val client = McpStdioClient(
                command = server.command!!,
                args = server.args ?: emptyList(),
                env = server.env
            )
            // Запускаем процесс
            client.start()
            client
        }
    }

    /**
     * Остановить все локальные процессы
     */
    public suspend fun stopAllLocalServers() {
        stdioClients.values.forEach { client ->
            try {
                client.stop()
            } catch (e: Exception) {
                logger.warning("Error stopping stdio client: ${e.message}")
            }
        }
        stdioClients.clear()
        logger.info("Stopped all local MCP servers")
    }

    /**
     * Очистить кэш клиентов (например, при изменении списка серверов)
     */
    public suspend fun clearCache() {
        remoteClients.clear()
        stopAllLocalServers()
        logger.info("Cleared MCP clients cache")
    }
}