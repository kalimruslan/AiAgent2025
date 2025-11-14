package ru.llm.agent.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import ru.llm.agent.database.messages.MessageDao
import ru.llm.agent.database.messages.MessageReadDao
import ru.llm.agent.database.messages.MessageWriteDao
import ru.llm.agent.database.messages.MessageEntity
import ru.llm.agent.database.context.ContextDao
import ru.llm.agent.database.context.ContextReadDao
import ru.llm.agent.database.context.ContextWriteDao
import ru.llm.agent.database.context.ContextEntity
import ru.llm.agent.database.expert.ExpertOpinionDao
import ru.llm.agent.database.expert.ExpertOpinionReadDao
import ru.llm.agent.database.expert.ExpertOpinionWriteDao
import ru.llm.agent.database.expert.ExpertOpinionEntity

@Database(
    entities = [MessageEntity::class, ContextEntity::class, ExpertOpinionEntity::class],
    version = 5,
    exportSchema = true
)
@ConstructedBy(MessageDatabaseConstructor::class)
public abstract class MessageDatabase : RoomDatabase() {
    // Старые DAO (для обратной совместимости)
    public abstract fun messageDao(): MessageDao
    public abstract fun settingsDao(): ContextDao
    public abstract fun expertOpinionDao(): ExpertOpinionDao

    // Новые разделённые DAO (read/write separation)
    public abstract fun messageReadDao(): MessageReadDao
    public abstract fun messageWriteDao(): MessageWriteDao
    public abstract fun contextReadDao(): ContextReadDao
    public abstract fun contextWriteDao(): ContextWriteDao
    public abstract fun expertOpinionReadDao(): ExpertOpinionReadDao
    public abstract fun expertOpinionWriteDao(): ExpertOpinionWriteDao

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

        /** Миграция 2 -> 3: Добавление таблицы expert_opinions для хранения мнений экспертов */
        public val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expert_opinions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        expertId TEXT NOT NULL,
                        expertName TEXT NOT NULL,
                        expertIcon TEXT NOT NULL,
                        messageId INTEGER NOT NULL,
                        conversationId TEXT NOT NULL,
                        opinion TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        originalResponse TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        /** Миграция 3 -> 4: Добавление полей для метаданных LLM (токены и время ответа) */
        public val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE messages ADD COLUMN inputTokens INTEGER DEFAULT NULL"
                )
                connection.execSQL(
                    "ALTER TABLE messages ADD COLUMN completionTokens INTEGER DEFAULT NULL"
                )
                connection.execSQL(
                    "ALTER TABLE messages ADD COLUMN totalTokens INTEGER DEFAULT NULL"
                )
                connection.execSQL(
                    "ALTER TABLE messages ADD COLUMN responseTimeMs INTEGER DEFAULT NULL"
                )
            }
        }

        /** Миграция 4 -> 5: Добавление поля isSummarized для суммаризации истории */
        public val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE messages ADD COLUMN isSummarized INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}

/** Конструктор для Room Database (требуется для KMP) */
@Suppress("NO_ACTUAL_FOR_EXPECT")
public expect object MessageDatabaseConstructor : RoomDatabaseConstructor<MessageDatabase>