package ru.llm.agent.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

public actual class DatabaseDriverFactory(private val context: Context) {
    public actual fun createDatabase(): MessageDatabase {
        val dbFile = context.getDatabasePath(MessageDatabase.DATABASE_NAME)
        return Room.databaseBuilder<MessageDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}