package ru.llm.agent.database.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public interface SettingsDao {
    @Query("SELECT * FROM settings")
    public suspend fun getSettings(): SettingsEntity

    @Query("SELECT * FROM settings ORDER BY id DESC LIMIT 1")
    public suspend fun getLastSettings(): SettingsEntity?

    @Insert
    public suspend fun insertSettings(message: SettingsEntity): Long

    @Query("DELETE FROM settings")
    public suspend fun deleteSettings()
}