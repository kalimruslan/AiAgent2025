package ru.llm.agent.database.context

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.ktor.util.debug.useContextElementInDebugMode
import ru.llm.agent.database.messages.MessageEntity

@Entity(tableName = "context")
public data class ContextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val temperature: Double,
    val systemprompt: String,
    val maxTokens: Int,
    val timestamp: Long
)