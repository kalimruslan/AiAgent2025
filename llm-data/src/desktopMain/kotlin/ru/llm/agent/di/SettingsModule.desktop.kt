package ru.llm.agent.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

/**
 * Desktop-реализация Settings через java.util.prefs.Preferences
 */
public actual val platformSettingsModule: Module = module {
    single<Settings> {
        val preferences = Preferences.userRoot().node("ru/llm/agent/settings")
        PreferencesSettings(preferences)
    }
}