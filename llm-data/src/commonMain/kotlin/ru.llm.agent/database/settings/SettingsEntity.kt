package ru.llm.agent.database.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
public data class SettingsEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperature: Double,
    val systemprompt: String,
    val maxTokens: Int,
    val timestamp: Long
)