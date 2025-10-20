package ru.llm.agent.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
public data class MessageEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val role: String, // "system", "user", "assistant"
    val text: String,
    val timestamp: Long
)