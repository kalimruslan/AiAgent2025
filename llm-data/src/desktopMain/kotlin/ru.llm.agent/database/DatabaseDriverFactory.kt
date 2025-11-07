package ru.llm.agent.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

public actual class DatabaseDriverFactory {
    public actual fun createDatabase(): MessageDatabase {
        val dbFile =
            File(System.getProperty("user.home"), ".chatapp/${MessageDatabase.DATABASE_NAME}")
        dbFile.parentFile?.mkdirs()

        return Room.databaseBuilder<MessageDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MessageDatabase.MIGRATION_1_2)
            .build()
    }
}