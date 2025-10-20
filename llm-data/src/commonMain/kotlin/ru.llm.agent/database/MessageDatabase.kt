package ru.llm.agent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.llm.agent.database.settings.SettingsDao
import ru.llm.agent.database.settings.SettingsEntity

@Database(
    entities = [MessageEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true
)
public abstract class MessageDatabase : RoomDatabase() {
    public abstract fun messageDao(): MessageDao
    public abstract fun settingsDao(): SettingsDao

    public companion object {
        public const val DATABASE_NAME: String = "messages.db"
    }
}