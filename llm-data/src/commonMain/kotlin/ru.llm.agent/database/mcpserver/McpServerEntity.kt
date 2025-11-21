package ru.llm.agent.database.mcpserver

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения информации об удаленных MCP серверах
 */
@Entity(tableName = "mcp_servers")
public data class McpServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Название сервера (для отображения) */
    val name: String,

    /** URL удаленного MCP сервера */
    val url: String,

    /** Описание сервера */
    val description: String? = null,

    /** Активен ли сервер (будет ли использоваться агентом) */
    val isActive: Boolean = true,

    /** Время добавления сервера */
    val timestamp: Long = System.currentTimeMillis()
)