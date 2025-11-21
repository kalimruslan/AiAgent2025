package ru.llm.agent.database.mcpserver

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения информации о MCP серверах (удаленных и локальных)
 */
@Entity(tableName = "mcp_servers")
public data class McpServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Название сервера (для отображения) */
    val name: String,

    /** Тип сервера: REMOTE или LOCAL */
    val type: String = "REMOTE",

    /** URL удаленного MCP сервера (для REMOTE) */
    val url: String? = null,

    /** Команда запуска (для LOCAL) */
    val command: String? = null,

    /** Аргументы команды в формате JSON (для LOCAL) */
    val args: String? = null,

    /** Переменные окружения в формате JSON (для LOCAL) */
    val env: String? = null,

    /** Описание сервера */
    val description: String? = null,

    /** Активен ли сервер (будет ли использоваться агентом) */
    val isActive: Boolean = true,

    /** Время добавления сервера */
    val timestamp: Long = System.currentTimeMillis()
)