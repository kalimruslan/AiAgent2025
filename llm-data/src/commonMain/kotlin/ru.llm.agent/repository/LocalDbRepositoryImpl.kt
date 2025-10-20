package ru.llm.agent.repository

import ru.llm.agent.database.settings.SettingsDao
import ru.llm.agent.model.Settings
import ru.llm.agent.toEntity
import ru.llm.agent.toModel

public class LocalDbRepositoryImpl(
    private val settingsDao: SettingsDao
): LocalDbRepository {

    override suspend fun saveSettings(settings: Settings){
        settingsDao.insertSettings(settings.toEntity())
    }

    override suspend fun getSettings(): Settings? {
        return settingsDao.getLastSettings()?.toModel()
    }
}