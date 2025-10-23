package ru.llm.agent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.llm.agent.database.messages.MessageDao
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.context.ContextEntity

@Database(
    entities = [MessageEntity::class, ContextEntity::class],
    version = 1,
    exportSchema = true
)
public abstract class MessageDatabase : RoomDatabase() {
    public abstract fun messageDao(): MessageDao
    public abstract fun settingsDao(): ContextDao

    public companion object {
        public const val DATABASE_NAME: String = "messages.db"
    }
}