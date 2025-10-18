package ru.llm.agent.database

public expect class DatabaseDriverFactory {
    public fun createDatabase(): MessageDatabase
}