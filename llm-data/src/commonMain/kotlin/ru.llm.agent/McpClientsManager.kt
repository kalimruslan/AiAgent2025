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

    // Кэш маппинга "имя инструмента → ID сервера"
    private val toolToServerMap = mutableMapOf<String, Long>()

    /**
     * Получить все инструменты со всех активных серверов (удаленных и локальных)
     */
    public suspend fun getAllTools(): Flow<List<McpTool>> {
        return mcpServerRepository.getActiveServers().map { activeServers ->
            val allTools = mutableListOf<McpTool>()

            // Очищаем старый маппинг
            toolToServerMap.clear()

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

                    // Добавляем инструменты в общий список
                    allTools.addAll(tools)

                    // Сохраняем маппинг "инструмент → сервер"
                    tools.forEach { tool ->
                        toolToServerMap[tool.name] = server.id
                        logger.info("Mapped tool '${tool.name}' to server '${server.name}' (id=${server.id})")
                    }

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
        // Проверяем, есть ли маппинг для этого инструмента
        val serverId = toolToServerMap[name]

        if (serverId != null) {
            // Инструмент найден в кэше, вызываем напрямую на нужном сервере
            logger.info("Tool '$name' mapped to server with id=$serverId, calling directly")

            val activeServers = mcpServerRepository.getActiveServers().first()
            val server = activeServers.find { it.id == serverId }

            if (server != null) {
                try {
                    logger.info("Calling tool '$name' on ${server.type} server: ${server.name}")

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

                    logger.info("Successfully called tool '$name' on ${server.name}")
                    return result
                } catch (e: Exception) {
                    logger.warning("Failed to call tool '$name' on server '${server.name}': ${e.message}")
                    // Fallback: попробуем все серверы
                }
            }
        }

        // Fallback: если маппинга нет или вызов не удался, пробуем все серверы
        logger.info("Tool '$name' not in cache or failed, trying all active servers")
        val activeServers = mcpServerRepository.getActiveServers().first()

        for (server in activeServers) {
            try {
                logger.info("Trying to call tool '$name' on ${server.type} server: ${server.name}")

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

                logger.info("Successfully called tool '$name' on ${server.name}")

                // Обновляем маппинг, если нашли инструмент
                toolToServerMap[name] = server.id

                return result
            } catch (e: Exception) {
                logger.info("Tool '$name' not found on ${server.name}, trying next server... Error: ${e.message}")
            }
        }

        throw IllegalArgumentException("Tool '$name' not found on any active MCP server")
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
        toolToServerMap.clear()
        logger.info("Cleared MCP clients cache and tool mappings")
    }
}