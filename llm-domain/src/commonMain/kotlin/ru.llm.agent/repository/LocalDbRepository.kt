package ru.llm.agent.repository

import ru.llm.agent.model.Settings

public interface LocalDbRepository{
    public suspend fun saveSettings(settings: Settings)
    public suspend fun getSettings(): Settings?
}