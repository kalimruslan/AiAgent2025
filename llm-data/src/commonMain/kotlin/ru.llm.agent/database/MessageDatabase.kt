package ru.llm.agent.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import ru.llm.agent.database.messages.MessageDao
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.context.ContextEntity

@Database(
    entities = [MessageEntity::class, ContextEntity::class],
    version = 2,
    exportSchema = true
)
@ConstructedBy(MessageDatabaseConstructor::class)
public abstract class MessageDatabase : RoomDatabase() {
    public abstract fun messageDao(): MessageDao
    public abstract fun settingsDao(): ContextDao

    public companion object {
        public const val DATABASE_NAME: String = "messages.db"

        /** Миграция 1 -> 2: Добавление поля llmProvider в таблицу context */
        public val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE context ADD COLUMN llmProvider TEXT DEFAULT NULL"
                )
            }
        }
    }
}

/** Конструктор для Room Database (требуется для KMP) */
@Suppress("NO_ACTUAL_FOR_EXPECT")
public expect object MessageDatabaseConstructor : RoomDatabaseConstructor<MessageDatabase>