package ru.llm.agent.repository

import kotlinx.serialization.json.JsonObject
import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.model.mcp.YaGptTool

/**
 * Репозиторий для работы с MCP (Model Context Protocol) инструментами.
 * Предоставляет доступ к external tools через MCP сервер для function calling.
 */
public interface McpRepository{
    /**
     * Получить список всех доступных MCP инструментов
     *
     * @return Список инструментов в формате YandexGPT tools
     */
    public suspend fun getMcpToolsList(): List<YaGptTool>

    /**
     * Получить информацию о доступных MCP инструментах для UI
     *
     * @return Список инструментов с детальной информацией
     */
    public suspend fun getToolsInfo(): List<McpToolInfo>

    /**
     * Вызвать MCP инструмент с указанными аргументами
     *
     * @param name Имя инструмента для вызова
     * @param arguments Аргументы в формате JSON
     * @return Результат выполнения инструмента
     */
    public suspend fun callTool(name: String, arguments: JsonObject): String
}