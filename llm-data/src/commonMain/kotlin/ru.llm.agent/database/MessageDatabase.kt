package ru.llm.agent.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = true
)
public abstract class MessageDatabase : RoomDatabase() {
    public abstract fun messageDao(): MessageDao

    public companion object {
        public const val DATABASE_NAME: String = "messages.db"
    }
}